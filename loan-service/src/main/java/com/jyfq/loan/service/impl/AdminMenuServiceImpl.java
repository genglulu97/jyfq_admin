package com.jyfq.loan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.mapper.SysMenuMapper;
import com.jyfq.loan.mapper.SysRoleMenuMapper;
import com.jyfq.loan.model.dto.AdminMenuQueryDTO;
import com.jyfq.loan.model.dto.AdminMenuSaveDTO;
import com.jyfq.loan.model.entity.SysMenu;
import com.jyfq.loan.model.entity.SysRoleMenu;
import com.jyfq.loan.model.vo.AdminMenuVO;
import com.jyfq.loan.model.vo.OptionVO;
import com.jyfq.loan.service.AdminMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Admin menu management service implementation.
 */
@Service
@RequiredArgsConstructor
public class AdminMenuServiceImpl implements AdminMenuService {

    private static final long ROOT_PARENT_ID = 0L;
    private static final int MENU_TYPE_CATALOG = 1;
    private static final int MENU_TYPE_MENU = 2;
    private static final int MENU_TYPE_BUTTON = 3;
    private static final Pattern MENU_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_:.-]{1,63}$");

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<AdminMenuVO> listMenus(AdminMenuQueryDTO query) {
        AdminMenuQueryDTO safeQuery = query == null ? new AdminMenuQueryDTO() : query;
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(safeQuery.getKeyword())) {
            String keyword = safeQuery.getKeyword().trim();
            wrapper.and(w -> w.like(SysMenu::getMenuName, keyword)
                    .or()
                    .like(SysMenu::getMenuCode, keyword)
                    .or()
                    .like(SysMenu::getPermission, keyword));
        }
        if (StringUtils.hasText(safeQuery.getMenuName())) {
            wrapper.like(SysMenu::getMenuName, safeQuery.getMenuName().trim());
        }
        if (StringUtils.hasText(safeQuery.getMenuCode())) {
            wrapper.like(SysMenu::getMenuCode, safeQuery.getMenuCode().trim().toUpperCase(Locale.ROOT));
        }
        if (safeQuery.getMenuType() != null) {
            wrapper.eq(SysMenu::getMenuType, safeQuery.getMenuType());
        }
        if (safeQuery.getVisible() != null) {
            wrapper.eq(SysMenu::getVisible, safeQuery.getVisible());
        }
        if (safeQuery.getStatus() != null) {
            wrapper.eq(SysMenu::getStatus, safeQuery.getStatus());
        }
        wrapper.orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getSortOrder)
                .orderByAsc(SysMenu::getId);
        return buildTree(sysMenuMapper.selectList(wrapper));
    }

    @Override
    public AdminMenuVO getMenuDetail(Long id) {
        return toVO(getExistingMenu(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMenu(AdminMenuSaveDTO request) {
        Long parentId = normalizeParentId(request);
        ensureParentExists(parentId);
        String menuCode = normalizeMenuCode(request);
        ensureMenuCodeUnique(null, menuCode);

        SysMenu menu = new SysMenu();
        fillMenu(menu, request, parentId, menuCode);
        sysMenuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(Long id, AdminMenuSaveDTO request) {
        getExistingMenu(id);
        Long parentId = normalizeParentId(request);
        if (id.equals(parentId)) {
            throw new BizException("上级菜单不能选择自己");
        }
        ensureParentExists(parentId);
        ensureNotDescendant(id, parentId);
        String menuCode = normalizeMenuCode(request);
        ensureMenuCodeUnique(id, menuCode);

        SysMenu menu = new SysMenu();
        menu.setId(id);
        fillMenu(menu, request, parentId, menuCode);
        sysMenuMapper.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        getExistingMenu(id);
        Long childCount = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException("请先删除子菜单");
        }
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
        sysMenuMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleMenu(Long id) {
        SysMenu existing = getExistingMenu(id);
        int targetStatus = Integer.valueOf(1).equals(existing.getStatus()) ? 0 : 1;
        sysMenuMapper.update(null, new LambdaUpdateWrapper<SysMenu>()
                .eq(SysMenu::getId, id)
                .set(SysMenu::getStatus, targetStatus));
    }

    @Override
    public List<OptionVO> listMenuOptions() {
        List<SysMenu> menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getSortOrder)
                .orderByAsc(SysMenu::getId));
        List<AdminMenuVO> tree = buildTree(menus);
        List<OptionVO> options = new ArrayList<>();
        flattenOptions(tree, 0, options);
        return options;
    }

    private SysMenu getExistingMenu(Long id) {
        if (id == null) {
            throw new BizException("菜单ID不能为空");
        }
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            throw new BizException("菜单不存在: " + id);
        }
        return menu;
    }

    private void fillMenu(SysMenu menu, AdminMenuSaveDTO request, Long parentId, String menuCode) {
        if (request == null || !StringUtils.hasText(request.getMenuName())) {
            throw new BizException("菜单名称不能为空");
        }
        menu.setParentId(parentId);
        menu.setMenuName(request.getMenuName().trim());
        menu.setMenuCode(menuCode);
        menu.setMenuType(defaultMenuType(request.getMenuType()));
        menu.setPath(trimToNull(request.getPath()));
        menu.setComponent(trimToNull(request.getComponent()));
        menu.setPermission(trimToNull(request.getPermission()));
        menu.setIcon(trimToNull(request.getIcon()));
        menu.setSortOrder(request.getSort() == null ? 0 : request.getSort());
        menu.setVisible(request.getVisible() == null ? 1 : request.getVisible());
        menu.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        menu.setRemark(trimToNull(request.getRemark()));
    }

    private Long normalizeParentId(AdminMenuSaveDTO request) {
        if (request == null || request.getParentId() == null || request.getParentId() < 0) {
            return ROOT_PARENT_ID;
        }
        return request.getParentId();
    }

    private String normalizeMenuCode(AdminMenuSaveDTO request) {
        if (request == null || !StringUtils.hasText(request.getMenuCode())) {
            throw new BizException("菜单编码不能为空");
        }
        String menuCode = request.getMenuCode().trim().toUpperCase(Locale.ROOT);
        if (!MENU_CODE_PATTERN.matcher(menuCode).matches()) {
            throw new BizException("菜单编码只能使用大写字母、数字、下划线、冒号、点或横线，且以字母开头");
        }
        return menuCode;
    }

    private Integer defaultMenuType(Integer menuType) {
        if (menuType == null) {
            return MENU_TYPE_MENU;
        }
        if (menuType != MENU_TYPE_CATALOG && menuType != MENU_TYPE_MENU && menuType != MENU_TYPE_BUTTON) {
            throw new BizException("菜单类型只能是1、2、3");
        }
        return menuType;
    }

    private void ensureParentExists(Long parentId) {
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            return;
        }
        if (sysMenuMapper.selectById(parentId) == null) {
            throw new BizException("上级菜单不存在: " + parentId);
        }
    }

    private void ensureNotDescendant(Long id, Long parentId) {
        Long currentParentId = parentId;
        while (currentParentId != null && currentParentId != ROOT_PARENT_ID) {
            if (id.equals(currentParentId)) {
                throw new BizException("上级菜单不能选择自己的子菜单");
            }
            SysMenu parent = sysMenuMapper.selectById(currentParentId);
            if (parent == null) {
                return;
            }
            currentParentId = parent.getParentId();
        }
    }

    private void ensureMenuCodeUnique(Long id, String menuCode) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getMenuCode, menuCode);
        if (id != null) {
            wrapper.ne(SysMenu::getId, id);
        }
        if (sysMenuMapper.selectCount(wrapper) > 0) {
            throw new BizException("菜单编码已存在: " + menuCode);
        }
    }

    private List<AdminMenuVO> buildTree(List<SysMenu> menus) {
        Map<Long, AdminMenuVO> nodeMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            nodeMap.put(menu.getId(), toVO(menu));
        }

        List<AdminMenuVO> roots = new ArrayList<>();
        for (AdminMenuVO node : nodeMap.values()) {
            AdminMenuVO parent = nodeMap.get(node.getParentId());
            if (parent == null || node.getParentId() == null || node.getParentId() == ROOT_PARENT_ID) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private void flattenOptions(List<AdminMenuVO> nodes, int depth, List<OptionVO> options) {
        String prefix = "  ".repeat(Math.max(0, depth));
        for (AdminMenuVO node : nodes) {
            options.add(new OptionVO(prefix + node.getMenuName(), String.valueOf(node.getId())));
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                flattenOptions(node.getChildren(), depth + 1, options);
            }
        }
    }

    private AdminMenuVO toVO(SysMenu menu) {
        AdminMenuVO vo = new AdminMenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuCode(menu.getMenuCode());
        vo.setMenuType(menu.getMenuType());
        vo.setMenuTypeDesc(menuTypeDesc(menu.getMenuType()));
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setPermission(menu.getPermission());
        vo.setIcon(menu.getIcon());
        vo.setSort(menu.getSortOrder());
        vo.setVisible(menu.getVisible());
        vo.setVisibleDesc(Integer.valueOf(1).equals(menu.getVisible()) ? "显示" : "隐藏");
        vo.setStatus(menu.getStatus());
        vo.setStatusDesc(Integer.valueOf(1).equals(menu.getStatus()) ? "启用" : "禁用");
        vo.setRemark(menu.getRemark());
        vo.setCreatedAt(menu.getCreatedAt());
        vo.setUpdatedAt(menu.getUpdatedAt());
        return vo;
    }

    private String menuTypeDesc(Integer menuType) {
        if (Integer.valueOf(MENU_TYPE_CATALOG).equals(menuType)) {
            return "目录";
        }
        if (Integer.valueOf(MENU_TYPE_BUTTON).equals(menuType)) {
            return "按钮";
        }
        return "菜单";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
