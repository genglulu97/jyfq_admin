package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CrmCustomerBatchImportDTO implements Serializable {
    private List<CrmCustomerSaveDTO> customers;
    private Integer autoPublicPool;
    private Long assignToAdminId;
}
