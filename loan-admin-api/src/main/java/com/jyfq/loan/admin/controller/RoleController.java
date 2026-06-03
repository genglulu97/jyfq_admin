package com.jyfq.loan.admin.controller;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.AdminRoleMenuDTO;
import com.jyfq.loan.model.dto.AdminRoleQueryDTO;
import com.jyfq.loan.model.dto.AdminRoleSaveDTO;
import com.jyfq.loan.model.vo.AdminRoleVO;
import com.jyfq.loan.model.vo.OptionVO;
import com.jyfq.loan.service.AdminRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin role management APIs.
 */
@Tag(name = "角色权限")
@RestController
@RequestMapping({"/admin/role", "/admin/role-permission", "/admin/sys-role"})
@RequiredArgsConstructor
public class RoleController {

    private final AdminRoleService adminRoleService;

    @Operation(summary = "角色列表")
    @GetMapping("/list")
    public R<PageResult<AdminRoleVO>> list(AdminRoleQueryDTO query) {
        return R.ok(adminRoleService.pageRoles(query));
    }

    @Operation(summary = "角色详情")
    @GetMapping("/detail/{id}")
    public R<AdminRoleVO> detail(@PathVariable Long id) {
        return R.ok(adminRoleService.getRoleDetail(id));
    }

    @Operation(summary = "角色详情")
    @GetMapping("/detail")
    public R<AdminRoleVO> detailByParam(@RequestParam Long id) {
        return R.ok(adminRoleService.getRoleDetail(id));
    }

    @Operation(summary = "角色选项")
    @GetMapping("/options")
    public R<List<OptionVO>> options() {
        return R.ok(adminRoleService.listRoleOptions());
    }

    @Operation(summary = "新增角色")
    @PostMapping("/add")
    public R<Long> add(@Valid @RequestBody AdminRoleSaveDTO request) {
        return R.ok(adminRoleService.createRole(request));
    }

    @Operation(summary = "更新角色")
    @PutMapping("/update/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody AdminRoleSaveDTO request) {
        adminRoleService.updateRole(id, request);
        return R.ok();
    }

    @Operation(summary = "更新角色")
    @PutMapping("/update")
    public R<?> updateByParam(@RequestParam Long id, @Valid @RequestBody AdminRoleSaveDTO request) {
        adminRoleService.updateRole(id, request);
        return R.ok();
    }

    @Operation(summary = "分配菜单权限")
    @PutMapping("/assign-menus/{id}")
    public R<?> assignMenus(@PathVariable Long id, @RequestBody AdminRoleMenuDTO request) {
        adminRoleService.assignMenus(id, request);
        return R.ok();
    }

    @Operation(summary = "分配菜单权限")
    @PutMapping("/assign-menus")
    public R<?> assignMenusByParam(@RequestParam Long id, @RequestBody AdminRoleMenuDTO request) {
        adminRoleService.assignMenus(id, request);
        return R.ok();
    }

    @Operation(summary = "启停角色")
    @PutMapping("/toggle/{id}")
    public R<?> toggle(@PathVariable Long id) {
        adminRoleService.toggleRole(id);
        return R.ok();
    }

    @Operation(summary = "启停角色")
    @PutMapping("/toggle")
    public R<?> toggleByParam(@RequestParam Long id) {
        adminRoleService.toggleRole(id);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/delete/{id}")
    public R<?> delete(@PathVariable Long id) {
        adminRoleService.deleteRole(id);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/delete")
    public R<?> deleteByParam(@RequestParam Long id) {
        adminRoleService.deleteRole(id);
        return R.ok();
    }
}
