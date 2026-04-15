package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Admin institution query params.
 */
@Data
public class InstitutionQueryDTO implements Serializable {

    private Long current = 1L;

    private Long size = 10L;

    private String merchantType;

    private String merchantAlias;

    private Integer status;
}
