package com.jyfq.loan.service;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.model.dto.InstitutionReportQueryDTO;
import com.jyfq.loan.model.vo.InstitutionReportRowVO;
import com.jyfq.loan.model.vo.InstitutionReportSummaryVO;

import java.util.List;

/**
 * Admin institution report service.
 */
public interface AdminInstitutionReportService {

    InstitutionReportSummaryVO summary(InstitutionReportQueryDTO query);

    PageResult<InstitutionReportRowVO> page(InstitutionReportQueryDTO query);

    List<InstitutionReportRowVO> exportRows(InstitutionReportQueryDTO query);
}
