package com.jyfq.loan.admin.controller;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.InstitutionProductQueryDTO;
import com.jyfq.loan.model.dto.InstitutionProductSaveDTO;
import com.jyfq.loan.model.vo.InstitutionProductDetailVO;
import com.jyfq.loan.model.vo.InstitutionProductListVO;
import com.jyfq.loan.model.vo.InstitutionProductOptionsVO;
import com.jyfq.loan.service.AdminInstitutionProductService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin institution product management APIs.
 */
@Tag(name = "机构产品管理")
@RestController
@RequestMapping("/admin/institution-product")
@RequiredArgsConstructor
public class InstitutionProductController {

    private final AdminInstitutionProductService adminInstitutionProductService;

    @Operation(summary = "机构产品列表")
    @GetMapping("/list")
    public R<PageResult<InstitutionProductListVO>> list(InstitutionProductQueryDTO query) {
        return R.ok(adminInstitutionProductService.pageProducts(query));
    }

    @Operation(summary = "机构产品详情")
    @GetMapping("/detail/{id}")
    public R<InstitutionProductDetailVO> detail(@PathVariable Long id) {
        return R.ok(adminInstitutionProductService.getDetail(id));
    }

    @Operation(summary = "机构产品联动选项")
    @GetMapping("/options")
    public R<InstitutionProductOptionsVO> options() {
        return R.ok(adminInstitutionProductService.getOptions());
    }

    @Operation(summary = "新增机构产品")
    @PostMapping("/add")
    public R<Long> add(@Valid @RequestBody InstitutionProductSaveDTO request) {
        return R.ok(adminInstitutionProductService.createProduct(request));
    }

    @Operation(summary = "更新机构产品")
    @PutMapping("/update/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody InstitutionProductSaveDTO request) {
        adminInstitutionProductService.updateProduct(id, request);
        return R.ok();
    }

    @Operation(summary = "删除机构产品")
    @DeleteMapping("/delete/{id}")
    public R<?> delete(@PathVariable Long id) {
        adminInstitutionProductService.deleteProduct(id);
        return R.ok();
    }

    @Operation(summary = "复制机构产品")
    @PostMapping("/copy/{id}")
    public R<Long> copy(@PathVariable Long id) {
        return R.ok(adminInstitutionProductService.copyProduct(id));
    }
}
