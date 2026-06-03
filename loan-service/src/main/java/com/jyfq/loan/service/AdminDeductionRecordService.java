package com.jyfq.loan.service;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.model.dto.DeductionRecordQueryDTO;
import com.jyfq.loan.model.vo.DeductionRecordListVO;
import com.jyfq.loan.model.vo.DeductionRecordSummaryVO;

/**
 * Admin deduction record query service.
 */
public interface AdminDeductionRecordService {

    PageResult<DeductionRecordListVO> pageDeductionRecords(DeductionRecordQueryDTO query);

    DeductionRecordSummaryVO summary(DeductionRecordQueryDTO query);

    DeductionRecordListVO detail(String orderNo);
}
