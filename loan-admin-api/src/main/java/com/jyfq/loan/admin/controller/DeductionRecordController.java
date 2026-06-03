package com.jyfq.loan.admin.controller;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.DeductionRecordQueryDTO;
import com.jyfq.loan.model.vo.DeductionRecordListVO;
import com.jyfq.loan.model.vo.DeductionRecordSummaryVO;
import com.jyfq.loan.service.AdminDeductionRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin deduction record APIs.
 */
@Tag(name = "扣费记录")
@RestController
@RequestMapping("/admin/deduction-record")
@RequiredArgsConstructor
public class DeductionRecordController {

    private final AdminDeductionRecordService adminDeductionRecordService;

    @Operation(summary = "扣费记录列表")
    @GetMapping("/list")
    public R<PageResult<DeductionRecordListVO>> list(DeductionRecordQueryDTO query) {
        return R.ok(adminDeductionRecordService.pageDeductionRecords(query));
    }

    @Operation(summary = "扣费记录统计")
    @GetMapping("/summary")
    public R<DeductionRecordSummaryVO> summary(DeductionRecordQueryDTO query) {
        return R.ok(adminDeductionRecordService.summary(query));
    }

    @Operation(summary = "扣费记录详情")
    @GetMapping("/detail/{orderNo}")
    public R<DeductionRecordListVO> detail(@PathVariable String orderNo) {
        return R.ok(adminDeductionRecordService.detail(orderNo));
    }
}
