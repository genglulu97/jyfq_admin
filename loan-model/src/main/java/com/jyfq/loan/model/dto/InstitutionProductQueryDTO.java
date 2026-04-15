package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Institution product management query params.
 */
@Data
public class InstitutionProductQueryDTO implements Serializable {

    private Long current = 1L;

    private Long size = 10L;

    private Long instId;

    private String merchantAlias;

    private Integer status;

    private String cityKeyword;
}
