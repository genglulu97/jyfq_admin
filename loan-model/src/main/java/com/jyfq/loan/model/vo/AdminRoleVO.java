package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin role view object.
 */
@Data
public class AdminRoleVO implements Serializable {

    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    private Integer status;

    private String statusDesc;

    private Integer sort;

    private List<Long> menuIds;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
