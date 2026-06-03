package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Admin role query params.
 */
@Data
public class AdminRoleQueryDTO implements Serializable {

    private Long current = 1L;

    private Long size = 10L;

    private String keyword;

    private String roleName;

    private String roleCode;

    private Integer status;
}
