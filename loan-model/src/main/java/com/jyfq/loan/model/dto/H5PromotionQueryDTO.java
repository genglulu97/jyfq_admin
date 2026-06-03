package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * H5 promotion query params.
 */
@Data
public class H5PromotionQueryDTO implements Serializable {

    private Long current = 1L;

    private Long size = 10L;

    /** Channel name or channel code. */
    private String keyword;

    /** Promotion link keyword. */
    private String linkKeyword;

    private Integer status;

    /** yyyy-MM-dd or yyyy-MM-dd HH:mm:ss. */
    private String startDate;

    /** yyyy-MM-dd or yyyy-MM-dd HH:mm:ss. */
    private String endDate;
}
