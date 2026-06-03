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
    private String idCardPrefixFour;
    private Integer age;
    private String cityCode;
    private String workCity;
    /** Internal value: 1 male, 2 female, 0 unknown. */
    private Integer gender;
    /** Internal value: 1 employee, 2 freelancer, 3 business owner, 4 civil servant / state-owned enterprise. */
    private Integer profession;
    /** Internal value: actual representative Sesame score. */
    private Integer zhima;
    /** Internal value: 1 yes, 2 no. */
    private Integer house;
    /** Internal value: 1 yes, 2 no. */
    private Integer vehicle;
    private String vehicleStatus;
    private String vehicleValue;
    /** Internal value: 0 none, 1 <6 months, 2 6-12 months, 3 12+ months. */
    private Integer providentFund;
    /** Internal value: 0 none, 1 <6 months, 2 6-12 months, 3 12+ months. */
    private Integer socialSecurity;
    /** Internal value: 0 none, 1 <6 months, 2 6-12 months, 3 12+ months. */
    private Integer commercialInsurance;
    /** Internal value: 1 good credit, 2 currently overdue. */
    private Integer overdue;
    /** Internal value: actual amount, such as 30000/50000/100000/200000. */
    private Integer loanAmount;
    /** Internal value: actual months, such as 6/12/24/36. */
    private Integer loanTime;
    /** Reserved for downstream-returned customer level. Do not populate from upstream payload. */
    private String customerLevel;
    private String ip;
    private Map<String, Object> extraInfo;
}
