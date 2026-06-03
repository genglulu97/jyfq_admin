package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CrmPublicPoolQueryDTO implements Serializable {
    private Long current;
    private Long size;
    private String customerName;
    private String mobile;
    private String city;
    private String publicPoolReason;
    private String loanIntention;
    private Integer qualityStar;
}
