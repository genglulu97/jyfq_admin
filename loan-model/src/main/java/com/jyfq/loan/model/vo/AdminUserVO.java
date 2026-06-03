package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Admin user view object.
 */
@Data
public class AdminUserVO implements Serializable {

    private Long id;

    private String username;

    private String realName;

    private String role;

    private String roleName;

    private Integer status;

    private String statusDesc;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
