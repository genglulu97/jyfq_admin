package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CrmCustomerAssignDTO implements Serializable {
    @NotEmpty(message = "customerIds is required")
    private List<Long> customerIds;
    @NotNull(message = "toAdminId is required")
    private Long toAdminId;
    private String assignMode;
    private String remark;
}
