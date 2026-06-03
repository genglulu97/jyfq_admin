package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CrmInstitutionConfigQueryDTO implements Serializable {
    private Long current;
    private Long size;
    private Long instId;
    private String instCode;
    private String instName;
    private Long platformInstId;
    private String platformInstCode;
    private String platformInstName;
    private Long crmInstId;
    private String crmInstCode;
    private String crmInstName;
    private Integer status;
}
