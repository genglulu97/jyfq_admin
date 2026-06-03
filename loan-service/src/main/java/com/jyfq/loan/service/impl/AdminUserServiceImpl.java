package com.jyfq.loan.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.mapper.SysAdminMapper;
import com.jyfq.loan.mapper.SysRoleMapper;
import com.jyfq.loan.model.dto.AdminUserPasswordDTO;
import com.jyfq.loan.model.dto.AdminUserQueryDTO;
import com.jyfq.loan.model.dto.AdminUserSaveDTO;
import com.jyfq.loan.model.entity.SysAdmin;
import com.jyfq.loan.model.entity.SysRole;
import com.jyfq.loan.model.vo.AdminUserVO;
import com.jyfq.loan.model.vo.OptionVO;
import com.jyfq.loan.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Admin user management service implementation.
 */
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final Long DEFAULT_ADMIN_ID = 1L;
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_OPERATOR = "OPERATOR";
    private static final Set<String> BUILT_IN_ROLES = Set.of(ROLE_SUPER_ADMIN, ROLE_ADMIN, ROLE_OPERATOR);

    private final SysAdminMapper sysAdminMapper;
    private final SysRoleMapper sysRoleMapper;

    @Override
    public PageResult<AdminUserVO> pageUsers(AdminUserQueryDTO query) {
        AdminUserQueryDTO safeQuery = query == null ? new AdminUserQueryDTO() : query;
        long current = safeQuery.getCurrent() == null || safeQuery.getCurrent() < 1 ? 1L : safeQuery.getCurrent();
        long size = safeQuery.getSize() == null || safeQuery.getSize() < 1 ? 10L : Math.min(safeQuery.getSize(), 100L);

        LambdaQueryWrapper<SysAdmin> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(safeQuery.getKeyword())) {
            String keyword = safeQuery.getKeyword().trim();
            wrapper.and(w -> w.like(SysAdmin::getUsername, keyword)
                    .or()
                    .like(SysAdmin::getRealName, keyword));
        }
        if (StringUtils.hasText(safeQuery.getUsername())) {
            wrapper.like(SysAdmin::getUsername, safeQuery.getUsername().trim());
        }
        if (StringUtils.hasText(safeQuery.getRealName())) {
            wrapper.like(SysAdmin::getRealName, safeQuery.getRealName().trim());
        }
        if (StringUtils.hasText(safeQuery.getRole())) {
            wrapper.eq(SysAdmin::getRole, normalizeRole(safeQuery.getRole()));
        }
        if (safeQuery.getStatus() != null) {
            wrapper.eq(SysAdmin::getStatus, safeQuery.getStatus());
        }
        wrapper.orderByDesc(SysAdmin::getCreatedAt).orderByDesc(SysAdmin::getId);

        Page<SysAdmin> page = sysAdminMapper.selectPage(new Page<>(current, size), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResult.empty(current, size);
        }

        List<AdminUserVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public AdminUserVO getUserDetail(Long id) {
        return toVO(getExistingUser(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(AdminUserSaveDTO request) {
        String username = normalizeUsername(request);
        String password = trimToNull(request.getPassword());
        if (password == null) {
            throw new BizException("密码不能为空");
        }
        validatePassword(password);
        ensureUsernameUnique(null, username);

        SysAdmin user = new SysAdmin();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password));
        user.setRealName(trimToNull(request.getRealName()));
        user.setRole(normalizeRoleOrDefault(request.getRole()));
        user.setStatus(defaultStatus(request.getStatus()));
        sysAdminMapper.insert(user);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, AdminUserSaveDTO request) {
        getExistingUser(id);
        String username = normalizeUsername(request);
        ensureUsernameUnique(id, username);
        Integer status = defaultStatus(request.getStatus());
        if (DEFAULT_ADMIN_ID.equals(id) && Integer.valueOf(0).equals(status)) {
            throw new BizException("默认管理员不能禁用");
        }

        SysAdmin user = new SysAdmin();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(trimToNull(request.getRealName()));
        user.setRole(normalizeRoleOrDefault(request.getRole()));
        user.setStatus(status);

        String password = trimToNull(request.getPassword());
        if (password != null) {
            validatePassword(password);
            user.setPassword(BCrypt.hashpw(password));
        }
        sysAdminMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        getExistingUser(id);
        if (DEFAULT_ADMIN_ID.equals(id)) {
            throw new BizException("默认管理员不能删除");
        }
        sysAdminMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleUser(Long id) {
        SysAdmin existing = getExistingUser(id);
        int targetStatus = Integer.valueOf(1).equals(existing.getStatus()) ? 0 : 1;
        if (DEFAULT_ADMIN_ID.equals(id) && targetStatus == 0) {
            throw new BizException("默认管理员不能禁用");
        }
        sysAdminMapper.update(null, new LambdaUpdateWrapper<SysAdmin>()
                .eq(SysAdmin::getId, id)
                .set(SysAdmin::getStatus, targetStatus)
                .set(SysAdmin::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, AdminUserPasswordDTO request) {
        getExistingUser(id);
        if (request == null || !StringUtils.hasText(request.getPassword())) {
            throw new BizException("密码不能为空");
        }
        String password = request.getPassword().trim();
        validatePassword(password);
        SysAdmin user = new SysAdmin();
        user.setId(id);
        user.setPassword(BCrypt.hashpw(password));
        sysAdminMapper.updateById(user);
    }

    @Override
    public List<OptionVO> listRoleOptions() {
        try {
            List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getStatus, 1)
                    .orderByAsc(SysRole::getSortOrder)
                    .orderByAsc(SysRole::getId));
            if (!roles.isEmpty()) {
                return roles.stream()
                        .map(role -> new OptionVO(role.getRoleName(), role.getRoleCode()))
                        .collect(Collectors.toList());
            }
        } catch (RuntimeException ignored) {
            // Keep user management usable before the role table upgrade is applied.
        }
        return defaultRoleOptions();
    }

    private SysAdmin getExistingUser(Long id) {
        if (id == null) {
            throw new BizException("用户ID不能为空");
        }
        SysAdmin existing = sysAdminMapper.selectById(id);
        if (existing == null) {
            throw new BizException("用户不存在: " + id);
        }
        return existing;
    }

    private void ensureUsernameUnique(Long id, String username) {
        LambdaQueryWrapper<SysAdmin> wrapper = new LambdaQueryWrapper<SysAdmin>()
                .eq(SysAdmin::getUsername, username);
        if (id != null) {
            wrapper.ne(SysAdmin::getId, id);
        }
        if (sysAdminMapper.selectCount(wrapper) > 0) {
            throw new BizException("用户名已存在: " + username);
        }
    }

    private String normalizeUsername(AdminUserSaveDTO request) {
        if (request == null || !StringUtils.hasText(request.getUsername())) {
            throw new BizException("用户名不能为空");
        }
        String username = request.getUsername().trim();
        if (username.length() > 32) {
            throw new BizException("用户名长度不能超过32");
        }
        return username;
    }

    private String normalizeRoleOrDefault(String role) {
        if (!StringUtils.hasText(role)) {
            return ROLE_OPERATOR;
        }
        return normalizeRole(role);
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        try {
            SysRole existing = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, normalized)
                    .last("LIMIT 1"));
            if (existing != null) {
                if (!Integer.valueOf(1).equals(existing.getStatus())) {
                    throw new BizException("角色已禁用: " + role);
                }
                return existing.getRoleCode();
            }
        } catch (BizException e) {
            throw e;
        } catch (RuntimeException ignored) {
            if (BUILT_IN_ROLES.contains(normalized)) {
                return normalized;
            }
        }
        throw new BizException("角色不支持: " + role);
    }

    private Integer defaultStatus(Integer status) {
        return status == null ? 1 : status;
    }

    private void validatePassword(String password) {
        if (password.length() < 6 || password.length() > 64) {
            throw new BizException("密码长度必须在6到64之间");
        }
    }

    private AdminUserVO toVO(SysAdmin user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setRoleName(roleName(user.getRole()));
        vo.setStatus(user.getStatus());
        vo.setStatusDesc(Integer.valueOf(1).equals(user.getStatus()) ? "启用" : "禁用");
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }

    private String roleName(String role) {
        try {
            SysRole existing = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, role)
                    .last("LIMIT 1"));
            if (existing != null && StringUtils.hasText(existing.getRoleName())) {
                return existing.getRoleName();
            }
        } catch (RuntimeException ignored) {
            // Fall through to built-in role names.
        }
        if (ROLE_SUPER_ADMIN.equals(role)) {
            return "超级管理员";
        }
        if (ROLE_ADMIN.equals(role)) {
            return "管理员";
        }
        if (ROLE_OPERATOR.equals(role)) {
            return "操作员";
        }
        return role;
    }

    private List<OptionVO> defaultRoleOptions() {
        return List.of(
                new OptionVO("超级管理员", ROLE_SUPER_ADMIN),
                new OptionVO("管理员", ROLE_ADMIN),
                new OptionVO("操作员", ROLE_OPERATOR)
        );
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
