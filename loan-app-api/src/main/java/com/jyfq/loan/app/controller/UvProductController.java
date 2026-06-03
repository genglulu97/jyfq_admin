package com.jyfq.loan.app.controller;

import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.UvProductDeleteDTO;
import com.jyfq.loan.model.dto.UvProductQueryDTO;
import com.jyfq.loan.model.dto.UvProductSaveDTO;
import com.jyfq.loan.model.vo.UvProductPageVO;
import com.jyfq.loan.service.UvProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UV product management APIs.
 */
@Tag(name = "UV Product")
@RestController
@RequestMapping("/api/uv/product")
@RequiredArgsConstructor
public class UvProductController {

    private final UvProductService uvProductService;

    @Operation(summary = "List UV products")
    @GetMapping("/list")
    public R<UvProductPageVO> list(UvProductQueryDTO query) {
        return R.ok(uvProductService.pageProducts(query));
    }

    @Operation(summary = "Add UV product")
    @PostMapping("/add")
    public R<?> add(@Valid @RequestBody UvProductSaveDTO request) {
        uvProductService.addProduct(request);
        return R.ok();
    }

    @Operation(summary = "Update UV product")
    @PostMapping("/update")
    public R<?> update(@Valid @RequestBody UvProductSaveDTO request) {
        uvProductService.updateProduct(request);
        return R.ok();
    }

    @Operation(summary = "Delete UV products")
    @PostMapping("/delete")
    public R<?> delete(@Valid @RequestBody UvProductDeleteDTO request) {
        uvProductService.deleteProducts(request);
        return R.ok();
    }
}
