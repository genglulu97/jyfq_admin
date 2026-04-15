package com.jyfq.loan.model.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Standardized internal application data.
 */
@Data
@Builder
public class StandardApplyData implements Serializable {

    private String channelCode;
    private String name;
    private String phone;
    private String phoneMd5;
    private String idCard;
    private Integer age;
    private String cityCode;
    private String workCity;
    private Integer gender;
    private Integer profession;
    private Integer zhima;
    private Integer house;
    private Integer vehicle;
    private String vehicleStatus;
    private String vehicleValue;
    private Integer providentFund;
    private Integer socialSecurity;
    private Integer commercialInsurance;
    private Integer overdue;
    private Integer loanAmount;
    private Integer loanTime;
    private String customerLevel;
    private String ip;
    private Map<String, Object> extraInfo;
}
