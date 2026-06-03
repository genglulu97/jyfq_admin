package com.jyfq.loan.service;

import com.jyfq.loan.model.dto.UvProductDeleteDTO;
import com.jyfq.loan.model.dto.UvProductQueryDTO;
import com.jyfq.loan.model.dto.UvProductSaveDTO;
import com.jyfq.loan.model.vo.H5UvProductVO;
import com.jyfq.loan.model.vo.UvProductPageVO;

import java.util.List;

/**
 * UV product service.
 */
public interface UvProductService {

    UvProductPageVO pageProducts(UvProductQueryDTO query);

    void addProduct(UvProductSaveDTO request);

    void updateProduct(UvProductSaveDTO request);

    void deleteProducts(UvProductDeleteDTO request);

    List<H5UvProductVO> listH5Products();
}
