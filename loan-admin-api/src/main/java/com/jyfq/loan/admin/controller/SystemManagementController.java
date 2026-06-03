package com.jyfq.loan.admin.controller;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.AdminMenuQueryDTO;
import com.jyfq.loan.model.dto.AdminRoleQueryDTO;
import com.jyfq.loan.model.dto.AdminUserQueryDTO;
import com.jyfq.loan.model.vo.AdminMenuVO;
import com.jyfq.loan.model.vo.AdminRoleVO;
import com.jyfq.loan.model.vo.AdminUserVO;
import com.jyfq.loan.service.AdminMenuService;
import com.jyfq.loan.service.AdminRoleService;
import com.jyfq.loan.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Compatibility APIs for frontend system management routes.
 */
@Tag(name = "System Management")
@RestController
@RequestMapping("/admin/system")
@RequiredArgsConstructor
public class SystemManagementController {

    private final AdminUserService adminUserService;
    private final AdminRoleService adminRoleService;
    private final AdminMenuService adminMenuService;

    @Operation(summary = "System user list")
    @GetMapping("/user/list")
    public R<PageResult<AdminUserVO>> userList(AdminUserQueryDTO query) {
        return R.ok(adminUserService.pageUsers(query));
    }

    @Operation(summary = "System role list")
    @GetMapping("/role/list")
    public R<PageResult<AdminRoleVO>> roleList(AdminRoleQueryDTO query) {
        return R.ok(adminRoleService.pageRoles(query));
    }

    @Operation(summary = "System menu tree")
    @GetMapping({"/menu/list", "/menu/tree"})
    public R<List<AdminMenuVO>> menuTree(AdminMenuQueryDTO query) {
        return R.ok(adminMenuService.listMenus(query));
    }
}
