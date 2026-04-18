package com.jyfq.loan.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Common upstream decrypted payload.
 *
 * Public enum conventions:
 * gender: 0 unknown, 1 male, 2 female
 * profession: 1 employee, 2 freelancer, 3 business owner, 4 public servant
 * duration-like fields (provident/social/commercialInsurance): 0 none, 1 <6m, 2 6-12m, 3 >=12m
 * house/vehicle: 1 yes, 2 no
 * overdue: 1 good credit, 2 overdue
 * loanAmount: accepts enum bucket 1/2/3/4 or actual amount
 * loanTime: accepts enum 2/3/4/5 or actual months
 */
@Data
public class CommonUpstreamPayloadDTO implements Serializable {

    private String name;
    private String phone;
    private String phoneMd5;
    private String idCard;
    private Integer age;
    private String city;
    private String cityCode;
    private String province;
    private String provinceCode;
    private Integer gender;
    private Integer loanTime;
    private Integer profession;
    private Integer zhima;
    private Integer providentFund;
    private Integer socialSecurity;
    @JsonAlias({"commericalInsurance"})
    private Integer commercialInsurance;
    private Integer house;
    private Integer overdue;
    private Integer vehicle;
    private Integer loanAmount;
    private String deviceIp;
    private String customerLevel;
    private Long productId;
    private String agreeProtocol;
    private String workCity;
    private Map<String, Object> extraInfo;
}
