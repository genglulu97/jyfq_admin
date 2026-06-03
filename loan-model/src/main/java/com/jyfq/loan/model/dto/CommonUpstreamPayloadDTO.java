package com.jyfq.loan.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Common upstream decrypted payload.
 *
 * Public upstream enum conventions:
 * gender: 1 male, 2 female, 0 unknown.
 * loanTime: 2=6 months, 3=12 months, 4=24 months, 5=36 months.
 * profession: 1 employee, 2 freelancer, 3 business owner, 4 civil servant / state-owned enterprise.
 * zhima: 1=600-650, 2=650-700, 3=700+, 4=none, 5=<600.
 * providentFund/socialSecurity: 1=<6 months, 2=6-12 months, 3=12+ months, 4=none.
 * commericalInsurance: 0=<6 months, 1=6-12 months, 2=12+ months, 3=none.
 * house/vehicle: 1 yes, 2 no.
 * overdue: 1 good credit, 2 currently overdue.
 * loanAmount: 1=30000, 2=50000, 3=100000, 4=200000.
 */
@Data
public class CommonUpstreamPayloadDTO implements Serializable {

    private String name;
    private String phone;
    private String phoneMd5;
    private String idCard;
    private String idCardPrefixFour;
    private Integer age;
    private String city;
    private String cityCode;
    private String province;
    private String provinceCode;
    /** Public upstream enum: 1 male, 2 female, 0 unknown. */
    private Integer gender;
    /** Public upstream enum: 2=6 months, 3=12 months, 4=24 months, 5=36 months. */
    private Integer loanTime;
    /** Public upstream enum: 1 employee, 2 freelancer, 3 business owner, 4 civil servant / state-owned enterprise. */
    private Integer profession;
    /** Public upstream enum: 1=600-650, 2=650-700, 3=700+, 4=none, 5=<600. */
    private Integer zhima;
    /** Public upstream enum: 1=<6 months, 2=6-12 months, 3=12+ months, 4=none. */
    private Integer providentFund;
    /** Public upstream enum: 1=<6 months, 2=6-12 months, 3=12+ months, 4=none. */
    private Integer socialSecurity;
    /** Public upstream enum: 0=<6 months, 1=6-12 months, 2=12+ months, 3=none. */
    @JsonAlias({"commericalInsurance"})
    private Integer commercialInsurance;
    /** Public upstream enum: 1 yes, 2 no. */
    private Integer house;
    /** Public upstream enum: 1 good credit, 2 currently overdue. */
    private Integer overdue;
    /** Public upstream enum: 1 yes, 2 no. */
    private Integer vehicle;
    /** Public upstream enum: 1=30000, 2=50000, 3=100000, 4=200000. */
    private Integer loanAmount;
    private String deviceIp;
    /** Ignored on upstream requests; customer level is reserved for downstream institution return. */
    private String customerLevel;
    private String collisionNo;
    private String localOrderNo;
    private String requestId;
    private Long productId;
    private String agreeProtocol;
    private String workCity;
    private Map<String, Object> extraInfo;
}
