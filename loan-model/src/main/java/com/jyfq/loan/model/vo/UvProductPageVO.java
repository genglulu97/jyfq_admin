package com.jyfq.loan.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * UV product page view matching frontend contract.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UvProductPageVO {

    private long total;
    private List<UvProductVO> list;
}
