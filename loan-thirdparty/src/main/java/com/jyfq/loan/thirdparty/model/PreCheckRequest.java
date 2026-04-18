package com.jyfq.loan.thirdparty.model;

import lombok.Data;

/**
 * Unified pre-check request.
 */
@Data
public class PreCheckRequest {

    private String phone;
    private String idCard;
    private String name;
    private String phoneMd5;
    private String idCardMd5;
    private Integer age;
    private String cityCode;
    private String workCity;
    private Integer amount;
    private Integer gender;
    private Integer loanTime;
    private Integer profession;
    private Integer zhima;
    private Integer providentFund;
    private Integer socialSecurity;
    private Integer commercialInsurance;
    private Integer house;
    private Integer overdue;
    private Integer vehicle;
    private Long productId;
    private String instCode;
}
