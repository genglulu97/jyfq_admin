package com.jyfq.loan.model.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serializable;

/**
 * 8-digit masked mobile pre-check payload.
 */
@Data
public class CommonUpstreamMobileEightPayloadDTO implements Serializable {

    private String requestId;
    private String mobileEight;
    private String phone;
    private Integer loanAmount;
    @JsonAlias({"city"})
    @JSONField(alternateNames = {"city"})
    private String cityName;
    private String ip;
    private Integer age;
    private Integer sex;
    private Integer hasHouse;
    private Integer hasCar;
    private Integer hasCompany;
    private Integer hasInsurance;
    private Integer hasSocial;
    private Integer hasFund;
    private Integer zmfScore;
    private Integer overdue;
}
