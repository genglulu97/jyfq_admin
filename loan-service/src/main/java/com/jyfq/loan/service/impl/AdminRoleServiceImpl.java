package com.jyfq.loan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.mapper.SysAdminMapper;
import com.jyfq.loan.mapper.SysMenuMapper;
import com.jyfq.loan.mapper.SysRoleMapper;
import com.jyfq.loan.mapper.SysRoleMenuMapper;
import com.jyfq.loan.model.dto.AdminRoleMenuDTO;
import com.jyfq.loan.model.dto.AdminRoleQueryDTO;
import com.jyfq.loan.model.dto.AdminRoleSaveDTO;
import com.jyfq.loan.model.entity.SysAdmin;
import com.jyfq.loan.model.entity.SysMenu;
import com.jyfq.loan.model.entity.SysRole;
import com.jyfq.loan.model.entity.SysRoleMenu;
import com.jyfq.loan.model.vo.AdminRoleVO;
import com.jyfq.loan.model.vo.OptionVO;
import com.jyfq.loan.service.AdminRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Admin role management service implementation.
 */
@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements AdminRoleService {

    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_OPERATOR = "OPERATOR";
    private static final Set<String> BUILT_IN_ROLES = Set.of(ROLE_SUPER_ADMIN, ROLE_ADMIN, ROLE_OPERATOR);
    private static final Pattern ROLE_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,31}$");

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysAdminMapper sysAdminMapper;

    @Override
    public PageResult<AdminRoleVO> pageRoles(AdminRoleQueryDTO query) {
        AdminRoleQueryDTO safeQuery = query == null ? new AdminRoleQueryDTO() : query;
        long current = safeQuery.getCurrent() == null || safeQuery.getCurrent() < 1 ? 1L : safeQuery.getCurrent();
        long size = safeQuery.getSize() == null || safeQuery.getSize() < 1 ? 10L : Math.min(safeQuery.getSize(), 100L);

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(safeQuery.getKeyword())) {
            String keyword = safeQuery.getKeyword().trim();
            wrapper.and(w -> w.like(SysRole::getRoleName, keyword)
                    .or()
                    .like(SysRole::getRoleCode, keyword));
        }
        if (StringUtils.hasText(safeQuery.getRoleName())) {
            wrapper.like(SysRole::getRoleName, safeQuery.getRoleName().trim());
        }
        if (StringUtils.hasText(safeQuery.getRoleCode())) {
            wrapper.like(SysRole::getRoleCode, safeQuery.getRoleCode().trim().toUpperCase(Locale.ROOT));
        }
        if (safeQuery.getStatus() != null) {
            wrapper.eq(SysRole::getStatus, safeQuery.getStatus());
        }
        wrapper.orderByAsc(SysRole::getSortOrder).orderByDesc(SysRole::getCreatedAt).orderByDesc(SysRole::getId);

        Page<SysRole> page = sysRoleMapper.selectPage(new Page<>(current, size), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResult.empty(current, size);
        }

        List<AdminRoleVO> records = page.getRecords().stream()
                .map(role -> toVO(role, false))
                .collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public AdminRoleVO getRoleDetail(Long id) {
        return toVO(getExistingRole(id), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(AdminRoleSaveDTO request) {
        String roleCode = normalizeRoleCode(request);
        ensureRoleCodeUnique(null, roleCode);

        SysRole role = new SysRole();
        fillRole(role, request, roleCode);
        sysRoleMapper.insert(role);
        assignMenuIds(role.getId(), request.getMenuIds());
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, AdminRoleSaveDTO request) {
        SysRole existing = getExistingRole(id);
        String roleCode = normalizeRoleCode(request);
        if (isBuiltInRole(existing.getRoleCode()) && !existing.getRoleCode().equals(roleCode)) {
            throw new BizException("内置角色编码不能修改");
        }
        if (ROLE_SUPER_ADMIN.equals(existing.getRoleCode()) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BizException("超级管理员角色不能禁用");
        }
        ensureRoleCodeUnique(id, roleCode);

        SysRole role = new SysRole();
        role.setId(id);
        fillRole(role, request, roleCode);
        sysRoleMapper.updateById(role);
        if (request.getMenuIds() != null) {
            assignMenuIds(id, request.getMenuIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SysRole existing = getExistingRole(id);
        if (isBuiltInRole(existing.getRoleCode())) {
            throw new BizException("内置角色不能删除");
        }
        Long userCount = sysAdminMapper.selectCount(new LambdaQueryWrapper<SysAdmin>()
                .eq(SysAdmin::getRole, existing.getRoleCode()));
        if (userCount != null && userCount > 0) {
            throw new BizException("角色已被用户使用，不能删除");
        }
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        sysRoleMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleRole(Long id) {
        SysRole existing = getExistingRole(id);
        int targetStatus = Integer.valueOf(1).equals(existing.getStatus()) ? 0 : 1;
        if (ROLE_SUPER_ADMIN.equals(existing.getRoleCode()) && targetStatus == 0) {
            throw new BizException("超级管理员角色不能禁用");
        }
        sysRoleMapper.update(null, new LambdaUpdateWrapper<SysRole>()
                .eq(SysRole::getId, id)
                .set(SysRole::getStatus, targetStatus));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long id, AdminRoleMenuDTO request) {
        getExistingRole(id);
        assignMenuIds(id, request == null ? null : request.getMenuIds());
    }

    @Override
    public List<OptionVO> listRoleOptions() {
        List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSortOrder)
                .orderByAsc(SysRole::getId));
        return roles.stream()
                .map(role -> new OptionVO(role.getRoleName(), role.getRoleCode()))
                .collect(Collectors.toList());
    }

    private SysRole getExistingRole(Long id) {
        if (id == null) {
            throw new BizException("角色ID不能为空");
        }
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BizException("角色不存在: " + id);
        }
        return role;
    }

    private void fillRole(SysRole role, AdminRoleSaveDTO request, String roleCode) {
        if (request == null || !StringUtils.hasText(request.getRoleName())) {
            throw new BizException("角色名称不能为空");
        }
        role.setRoleName(request.getRoleName().trim());
        role.setRoleCode(roleCode);
        role.setDescription(trimToNull(request.getDescription()));
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        role.setSortOrder(request.getSort() == null ? 0 : request.getSort());
    }

    private String normalizeRoleCode(AdminRoleSaveDTO request) {
        if (request == null || !StringUtils.hasText(request.getRoleCode())) {
            throw new BizException("角色编码不能为空");
        }
        String roleCode = request.getRoleCode().trim().toUpperCase(Locale.ROOT);
        if (!ROLE_CODE_PATTERN.matcher(roleCode).matches()) {
            throw new BizException("角色编码只能使用大写字母、数字、下划线，且以字母开头");
        }
        return roleCode;
    }

    private void ensureRoleCodeUnique(Long id, String roleCode) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode);
        if (id != null) {
            wrapper.ne(SysRole::getId, id);
        }
        if (sysRoleMapper.selectCount(wrapper) > 0) {
            throw new BizException("角色编码已存在: " + roleCode);
        }
    }

    private void assignMenuIds(Long roleId, List<Long> menuIds) {
        List<Long> normalizedMenuIds = normalizeMenuIds(menuIds);
        validateMenuIds(normalizedMenuIds);

        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        List<SysRoleMenu> relations = new ArrayList<>();
        for (Long menuId : normalizedMenuIds) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            relations.add(relation);
        }
        for (SysRoleMenu relation : relations) {
            sysRoleMenuMapper.insert(relation);
        }
    }

    private List<Long> normalizeMenuIds(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(menuIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private void validateMenuIds(List<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return;
        }
        Long count = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, menuIds));
        if (count == null || count != menuIds.size()) {
            throw new BizException("菜单权限包含不存在的节点");
        }
    }

    private AdminRoleVO toVO(SysRole role, boolean includeMenus) {
        AdminRoleVO vo = new AdminRoleVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleCode(role.getRoleCode());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setStatusDesc(Integer.valueOf(1).equals(role.getStatus()) ? "启用" : "禁用");
        vo.setSort(role.getSortOrder());
        vo.setCreatedAt(role.getCreatedAt());
        vo.setUpdatedAt(role.getUpdatedAt());
        if (includeMenus) {
            vo.setMenuIds(listMenuIds(role.getId()));
        }
        return vo;
    }

    private List<Long> listMenuIds(Long roleId) {
        return sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    private boolean isBuiltInRole(String roleCode) {
        return BUILT_IN_ROLES.contains(roleCode);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
