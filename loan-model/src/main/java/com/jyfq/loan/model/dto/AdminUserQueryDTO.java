package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Admin user query params.
 */
@Data
public class AdminUserQueryDTO implements Serializable {

    private Long current = 1L;

    private Long size = 10L;

    private String keyword;

    private String username;

    private String realName;

    private String role;

    private Integer status;
}
