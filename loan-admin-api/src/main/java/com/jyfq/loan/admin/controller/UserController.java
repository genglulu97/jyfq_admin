package com.jyfq.loan.admin.controller;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.AdminUserPasswordDTO;
import com.jyfq.loan.model.dto.AdminUserQueryDTO;
import com.jyfq.loan.model.dto.AdminUserSaveDTO;
import com.jyfq.loan.model.vo.AdminUserVO;
import com.jyfq.loan.model.vo.OptionVO;
import com.jyfq.loan.service.AdminUserService;
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
 * Admin user management APIs.
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping({"/admin/user", "/admin/sys-user"})
@RequiredArgsConstructor
public class UserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "用户列表")
    @GetMapping("/list")
    public R<PageResult<AdminUserVO>> list(AdminUserQueryDTO query) {
        return R.ok(adminUserService.pageUsers(query));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/detail/{id}")
    public R<AdminUserVO> detail(@PathVariable Long id) {
        return R.ok(adminUserService.getUserDetail(id));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/detail")
    public R<AdminUserVO> detailByParam(@RequestParam Long id) {
        return R.ok(adminUserService.getUserDetail(id));
    }

    @Operation(summary = "角色选项")
    @GetMapping("/role-options")
    public R<List<OptionVO>> roleOptions() {
        return R.ok(adminUserService.listRoleOptions());
    }

    @Operation(summary = "新增用户")
    @PostMapping("/add")
    public R<Long> add(@Valid @RequestBody AdminUserSaveDTO request) {
        return R.ok(adminUserService.createUser(request));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/update/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody AdminUserSaveDTO request) {
        adminUserService.updateUser(id, request);
        return R.ok();
    }

    @Operation(summary = "更新用户")
    @PutMapping("/update")
    public R<?> updateByParam(@RequestParam Long id, @Valid @RequestBody AdminUserSaveDTO request) {
        adminUserService.updateUser(id, request);
        return R.ok();
    }

    @Operation(summary = "启停用户")
    @PutMapping("/toggle/{id}")
    public R<?> toggle(@PathVariable Long id) {
        adminUserService.toggleUser(id);
        return R.ok();
    }

    @Operation(summary = "启停用户")
    @PutMapping("/toggle")
    public R<?> toggleByParam(@RequestParam Long id) {
        adminUserService.toggleUser(id);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/reset-password/{id}")
    public R<?> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminUserPasswordDTO request) {
        adminUserService.resetPassword(id, request);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/reset-password")
    public R<?> resetPasswordByParam(@RequestParam Long id, @Valid @RequestBody AdminUserPasswordDTO request) {
        adminUserService.resetPassword(id, request);
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/delete/{id}")
    public R<?> delete(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/delete")
    public R<?> deleteByParam(@RequestParam Long id) {
        adminUserService.deleteUser(id);
        return R.ok();
    }
}
