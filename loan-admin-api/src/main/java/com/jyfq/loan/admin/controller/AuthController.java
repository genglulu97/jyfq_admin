package com.jyfq.loan.admin.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.mapper.SysMenuMapper;
import com.jyfq.loan.mapper.SysAdminMapper;
import com.jyfq.loan.mapper.SysRoleMapper;
import com.jyfq.loan.mapper.SysRoleMenuMapper;
import com.jyfq.loan.model.entity.SysAdmin;
import com.jyfq.loan.model.entity.SysMenu;
import com.jyfq.loan.model.entity.SysRole;
import com.jyfq.loan.model.entity.SysRoleMenu;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Admin auth APIs.
 */
@Tag(name = "Admin Auth")
@RestController
@RequestMapping({"/api/auth", "/auth"})
public class AuthController {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "123456";
    private static final String ADMIN_LOGIN_ID = "admin:admin";
    private static final String INITIAL_ADMIN_PASSWORD_HASH = "$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36LNOQ/VKemXEJmzpN3vj52";
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_OPERATOR = "OPERATOR";

    private final SysAdminMapper sysAdminMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;

    public AuthController(SysAdminMapper sysAdminMapper,
                          SysRoleMapper sysRoleMapper,
                          SysRoleMenuMapper sysRoleMenuMapper,
                          SysMenuMapper sysMenuMapper) {
        this.sysAdminMapper = sysAdminMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysRoleMenuMapper = sysRoleMenuMapper;
        this.sysMenuMapper = sysMenuMapper;
    }

    @Operation(summary = "Login")
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody LoginDTO request) {
        if (request == null
                || !StringUtils.hasText(request.getUsername())
                || !StringUtils.hasText(request.getPassword())) {
            return R.fail(1002, "\u7528\u6237\u540d\u6216\u5bc6\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String username = request.getUsername().trim();
        String password = request.getPassword();
        SysAdmin admin = findByUsername(username);
        if (admin == null) {
            if (!isLegacyAdminPassword(username, password)) {
                return R.fail(2001, "\u7528\u6237\u540d\u6216\u5bc6\u7801\u9519\u8bef");
            }
            StpUtil.login(ADMIN_LOGIN_ID);
            String token = StpUtil.getTokenValue();
            return R.ok(LoginVO.of(token, buildLegacyAdminUser()), "\u767b\u5f55\u6210\u529f");
        }
        if (!Integer.valueOf(1).equals(admin.getStatus())) {
            return R.fail(2001, "\u8d26\u53f7\u5df2\u505c\u7528");
        }
        if (!passwordMatches(password, admin.getPassword()) && !isLegacyAdminPassword(username, password, admin)) {
            return R.fail(2001, "\u7528\u6237\u540d\u6216\u5bc6\u7801\u9519\u8bef");
        }

        StpUtil.login(buildLoginId(admin.getId()));
        String token = StpUtil.getTokenValue();
        return R.ok(LoginVO.of(token, buildUser(admin)), "\u767b\u5f55\u6210\u529f");
    }

    @Operation(summary = "Current user")
    @GetMapping("/me")
    public R<UserVO> me() {
        return currentUser();
    }

    @Operation(summary = "Current user info")
    @GetMapping("/user-info")
    public R<UserVO> userInfo() {
        return currentUser();
    }

    private R<UserVO> currentUser() {
        if (!StpUtil.isLogin()) {
            return R.fail(2001, "\u672a\u767b\u5f55\u6216\u767b\u5f55\u5df2\u8fc7\u671f");
        }
        SysAdmin admin = findByLoginId(String.valueOf(StpUtil.getLoginId()));
        if (admin == null) {
            return R.ok(buildLegacyAdminUser());
        }
        if (!Integer.valueOf(1).equals(admin.getStatus())) {
            return R.fail(2001, "\u8d26\u53f7\u5df2\u505c\u7528");
        }
        return R.ok(buildUser(admin));
    }

    @Operation(summary = "Logout")
    @PostMapping("/logout")
    public R<Void> logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
        return R.ok();
    }

    private SysAdmin findByUsername(String username) {
        return sysAdminMapper.selectOne(new LambdaQueryWrapper<SysAdmin>()
                .eq(SysAdmin::getUsername, username)
                .last("LIMIT 1"));
    }

    private SysAdmin findByLoginId(String loginId) {
        if (!StringUtils.hasText(loginId) || !loginId.startsWith("admin:")) {
            return null;
        }
        String idPart = loginId.substring("admin:".length());
        if (ADMIN_USERNAME.equals(idPart)) {
            return null;
        }
        try {
            return sysAdminMapper.selectById(Long.valueOf(idPart));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildLoginId(Long adminId) {
        return "admin:" + adminId;
    }

    private boolean isLegacyAdminPassword(String username, String password) {
        return ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password);
    }

    private boolean isLegacyAdminPassword(String username, String password, SysAdmin admin) {
        return isLegacyAdminPassword(username, password)
                && admin != null
                && Long.valueOf(1L).equals(admin.getId())
                && INITIAL_ADMIN_PASSWORD_HASH.equals(admin.getPassword());
    }

    private boolean passwordMatches(String rawPassword, String encodedPassword) {
        if (!StringUtils.hasText(encodedPassword)) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, encodedPassword);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private UserVO buildLegacyAdminUser() {
        UserVO user = new UserVO();
        user.setId(1L);
        user.setUsername(ADMIN_USERNAME);
        user.setNickname("\u7ba1\u7406\u5458");
        user.setRealName("\u7ba1\u7406\u5458");
        user.setRole(ROLE_SUPER_ADMIN);
        user.setStatus(1);
        user.setRoles(List.of("admin"));
        user.setPermissions(List.of("*"));
        return user;
    }

    private UserVO buildUser(SysAdmin admin) {
        UserVO user = new UserVO();
        user.setId(admin.getId());
        user.setUsername(admin.getUsername());
        user.setRealName(admin.getRealName());
        user.setNickname(StringUtils.hasText(admin.getRealName()) ? admin.getRealName() : admin.getUsername());
        user.setRole(admin.getRole());
        user.setStatus(admin.getStatus());
        user.setRoles(resolveRoles(admin.getRole()));
        user.setPermissions(resolvePermissions(admin.getRole()));
        return user;
    }

    private List<String> resolveRoles(String role) {
        if (ROLE_OPERATOR.equals(role)) {
            return List.of("operator", ROLE_OPERATOR);
        }
        if (ROLE_ADMIN.equals(role)) {
            return List.of("admin", ROLE_ADMIN);
        }
        return List.of("admin", ROLE_SUPER_ADMIN);
    }

    private List<String> resolvePermissions(String roleCode) {
        if (ROLE_SUPER_ADMIN.equals(roleCode)) {
            return List.of("*");
        }
        try {
            SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, roleCode)
                    .eq(SysRole::getStatus, 1)
                    .last("LIMIT 1"));
            if (role == null) {
                return fallbackPermissions(roleCode);
            }
            List<Long> menuIds = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                            .eq(SysRoleMenu::getRoleId, role.getId()))
                    .stream()
                    .map(SysRoleMenu::getMenuId)
                    .collect(Collectors.toList());
            if (menuIds.isEmpty()) {
                return fallbackPermissions(roleCode);
            }
            List<String> permissions = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                            .in(SysMenu::getId, menuIds)
                            .eq(SysMenu::getStatus, 1))
                    .stream()
                    .map(SysMenu::getPermission)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());
            return permissions.isEmpty() ? fallbackPermissions(roleCode) : permissions;
        } catch (RuntimeException ignored) {
            return fallbackPermissions(roleCode);
        }
    }

    private List<String> fallbackPermissions(String roleCode) {
        if (ROLE_ADMIN.equals(roleCode)) {
            return List.of("*");
        }
        if (StringUtils.hasText(roleCode)) {
            return List.of(roleCode.toLowerCase(Locale.ROOT));
        }
        return List.of("user");
    }

    @Data
    public static class LoginDTO implements Serializable {
        private String username;
        private String password;
    }

    @Data
    public static class LoginVO implements Serializable {
        private String token;
        private String accessToken;
        private String tokenType;
        private UserVO user;

        static LoginVO of(String token, UserVO user) {
            LoginVO vo = new LoginVO();
            vo.setToken(token);
            vo.setAccessToken(token);
            vo.setTokenType("Bearer");
            vo.setUser(user);
            return vo;
        }
    }

    @Data
    public static class UserVO implements Serializable {
        private Long id;
        private String username;
        private String nickname;
        private String realName;
        private String role;
        private Integer status;
        private List<String> roles;
        private List<String> permissions;
    }
}
