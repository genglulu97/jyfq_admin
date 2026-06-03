package com.jyfq.loan.admin.controller;

import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.AdminMenuQueryDTO;
import com.jyfq.loan.model.dto.AdminMenuSaveDTO;
import com.jyfq.loan.model.vo.AdminMenuVO;
import com.jyfq.loan.model.vo.OptionVO;
import com.jyfq.loan.service.AdminMenuService;
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
 * Admin menu management APIs.
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping({"/admin/menu", "/admin/sys-menu"})
@RequiredArgsConstructor
public class MenuController {

    private final AdminMenuService adminMenuService;

    @Operation(summary = "菜单树")
    @GetMapping({"/list", "/tree"})
    public R<List<AdminMenuVO>> list(AdminMenuQueryDTO query) {
        return R.ok(adminMenuService.listMenus(query));
    }

    @Operation(summary = "菜单选项")
    @GetMapping("/options")
    public R<List<OptionVO>> options() {
        return R.ok(adminMenuService.listMenuOptions());
    }

    @Operation(summary = "菜单详情")
    @GetMapping("/detail/{id}")
    public R<AdminMenuVO> detail(@PathVariable Long id) {
        return R.ok(adminMenuService.getMenuDetail(id));
    }

    @Operation(summary = "菜单详情")
    @GetMapping("/detail")
    public R<AdminMenuVO> detailByParam(@RequestParam Long id) {
        return R.ok(adminMenuService.getMenuDetail(id));
    }

    @Operation(summary = "新增菜单")
    @PostMapping("/add")
    public R<Long> add(@Valid @RequestBody AdminMenuSaveDTO request) {
        return R.ok(adminMenuService.createMenu(request));
    }

    @Operation(summary = "更新菜单")
    @PutMapping("/update/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody AdminMenuSaveDTO request) {
        adminMenuService.updateMenu(id, request);
        return R.ok();
    }

    @Operation(summary = "更新菜单")
    @PutMapping("/update")
    public R<?> updateByParam(@RequestParam Long id, @Valid @RequestBody AdminMenuSaveDTO request) {
        adminMenuService.updateMenu(id, request);
        return R.ok();
    }

    @Operation(summary = "启停菜单")
    @PutMapping("/toggle/{id}")
    public R<?> toggle(@PathVariable Long id) {
        adminMenuService.toggleMenu(id);
        return R.ok();
    }

    @Operation(summary = "启停菜单")
    @PutMapping("/toggle")
    public R<?> toggleByParam(@RequestParam Long id) {
        adminMenuService.toggleMenu(id);
        return R.ok();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/delete/{id}")
    public R<?> delete(@PathVariable Long id) {
        adminMenuService.deleteMenu(id);
        return R.ok();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/delete")
    public R<?> deleteByParam(@RequestParam Long id) {
        adminMenuService.deleteMenu(id);
        return R.ok();
    }
}
