package com.jyfq.loan.admin.controller;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.H5PromotionQueryDTO;
import com.jyfq.loan.model.vo.H5PromotionListVO;
import com.jyfq.loan.model.vo.H5PromotionSummaryVO;
import com.jyfq.loan.service.H5PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * H5 promotion management APIs.
 */
@Tag(name = "H5 promotion")
@RestController
@RequestMapping({"/admin/h5-promotion", "/admin/channel/h5"})
@RequiredArgsConstructor
public class H5PromotionController {

    private final H5PromotionService h5PromotionService;

    @Operation(summary = "H5 promotion list")
    @GetMapping("/list")
    public R<PageResult<H5PromotionListVO>> list(H5PromotionQueryDTO query) {
        return R.ok(h5PromotionService.pagePromotions(query));
    }

    @Operation(summary = "H5 promotion summary")
    @GetMapping("/summary")
    public R<H5PromotionSummaryVO> summary(H5PromotionQueryDTO query) {
        return R.ok(h5PromotionService.summary(query));
    }
}
