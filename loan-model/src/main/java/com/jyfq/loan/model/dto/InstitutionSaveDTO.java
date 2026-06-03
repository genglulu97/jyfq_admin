package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Institution create/update request.
 */
@Data
public class InstitutionSaveDTO implements Serializable {

    @NotBlank(message = "merchant name is required")
    private String instName;

    @NotBlank(message = "merchant alias is required")
    private String merchantAlias;

    @NotBlank(message = "merchant type is required")
    private String merchantType;

    @NotBlank(message = "channel type is required")
    private String channelType;

    private String businessOwner;
    private String remark;
    private String adminPhone;
    private String adminName;
    private String adminRole;
    private Integer smsNotify;
    private Integer userStatus;
    private Integer businessStatus;
    private Integer crmAutoAssign;
    private Integer apiMerchant;
    private String specifiedChannel;
    private String excludedChannels;
    private List<String> cityCodes;
    private String openCities;
    private String productName;
    private String productIcon;
    private Integer productAmount;
    private BigDecimal productRate;
    private Integer productPeriod;
    private String productProtocol;
}
