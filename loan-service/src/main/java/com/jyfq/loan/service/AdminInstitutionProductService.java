package com.jyfq.loan.service;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.model.dto.InstitutionProductQueryDTO;
import com.jyfq.loan.model.dto.InstitutionProductSaveDTO;
import com.jyfq.loan.model.vo.InstitutionProductDetailVO;
import com.jyfq.loan.model.vo.InstitutionProductListVO;
import com.jyfq.loan.model.vo.InstitutionProductOptionsVO;

/**
 * Admin institution product management service.
 */
public interface AdminInstitutionProductService {

    PageResult<InstitutionProductListVO> pageProducts(InstitutionProductQueryDTO query);

    InstitutionProductDetailVO getDetail(Long id);

    Long createProduct(InstitutionProductSaveDTO request);

    void updateProduct(Long id, InstitutionProductSaveDTO request);

    void deleteProduct(Long id);

    Long copyProduct(Long id);

    InstitutionProductOptionsVO getOptions();
}
