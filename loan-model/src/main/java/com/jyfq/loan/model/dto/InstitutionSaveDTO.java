package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * Institution create request.
 */
@Data
public class InstitutionSaveDTO implements Serializable {

    @NotBlank(message = "商户名称不能为空")
    private String instName;

    @NotBlank(message = "商户别名不能为空")
    private String merchantAlias;

    @NotBlank(message = "类型不能为空")
    private String merchantType;

    private String remark;
    private String adminPhone;
    private String adminName;
    private String adminRole;
    private Integer smsNotify;
    private Integer userStatus;
    private Integer businessStatus;
    private Integer crmAutoAssign;
    private Integer apiMerchant;
    private String apiMethodName;
    private String specifiedChannel;
    private String excludedChannels;
}
