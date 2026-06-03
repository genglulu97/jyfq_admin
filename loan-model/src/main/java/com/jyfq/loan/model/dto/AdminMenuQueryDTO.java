package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Admin menu query params.
 */
@Data
public class AdminMenuQueryDTO implements Serializable {

    private String keyword;

    private String menuName;

    private String menuCode;

    private Integer menuType;

    private Integer visible;

    private Integer status;
}
