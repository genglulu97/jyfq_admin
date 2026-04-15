package com.jyfq.loan.service;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.model.dto.InstitutionQueryDTO;
import com.jyfq.loan.model.dto.InstitutionRechargeDTO;
import com.jyfq.loan.model.dto.InstitutionSaveDTO;
import com.jyfq.loan.model.vo.InstitutionListVO;
import com.jyfq.loan.model.vo.InstitutionProductVO;
import com.jyfq.loan.model.vo.InstitutionRechargeRecordVO;
import com.jyfq.loan.model.vo.OptionVO;

import java.util.List;

/**
 * Admin institution management service.
 */
public interface AdminInstitutionService {

    PageResult<InstitutionListVO> pageInstitutions(InstitutionQueryDTO query);

    Long createInstitution(InstitutionSaveDTO request);

    void deleteInstitution(Long instId);

    List<OptionVO> listCityOptions();

    List<InstitutionProductVO> listProducts(Long instId);

    List<InstitutionRechargeRecordVO> listRechargeRecords(Long instId);

    void toggleInstitution(Long instId);

    void recharge(Long instId, InstitutionRechargeDTO request);
}
