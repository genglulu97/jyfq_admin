package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CrmCurrentUserDTO implements Serializable {
    private Long adminId;
    private String username;
    private String realName;
    private String role;
    private Long teamId;

    public boolean isAdmin() {
        return "SUPER_ADMIN".equals(role) || "ADMIN".equals(role);
    }

    public boolean isSupervisor() {
        return "SUPERVISOR".equals(role) || "MANAGER".equals(role);
    }

    public boolean isOperator() {
        return !isAdmin() && !isSupervisor();
    }
}
