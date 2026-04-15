package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * Application request DTO.
 */
@Data
public class ApplyRequestDTO implements Serializable {

    @NotBlank(message = "channelCode不能为空")
    private String channelCode;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "身份证号不能为空")
    private String idCard;

    @NotBlank(message = "姓名不能为空")
    private String userName;

    @NotNull(message = "年龄不能为空")
    @Min(value = 18, message = "年龄不能小于18岁")
    @Max(value = 65, message = "年龄不能大于65岁")
    private Integer age;

    @NotBlank(message = "城市编码不能为空")
    private String cityCode;

    private String workCity;

    @NotNull(message = "贷款金额不能为空")
    @Min(value = 1000, message = "贷款金额不能小于1000元")
    private Integer amount;

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
    private Integer loanTime;
    private String customerLevel;
    private String deviceIp;

    @NotBlank(message = "签名不能为空")
    private String sign;

    @NotNull(message = "时间戳不能为空")
    private Long timestamp;
}
