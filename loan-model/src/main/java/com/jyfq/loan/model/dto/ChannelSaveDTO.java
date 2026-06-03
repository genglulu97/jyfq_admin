package com.jyfq.loan.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Channel create/update request.
 */
@Data
public class ChannelSaveDTO implements Serializable {

    @NotBlank(message = "渠道ID不能为空")
    private String channelId;

    @NotBlank(message = "渠道名称不能为空")
    private String channelName;

    @NotBlank(message = "渠道标识不能为空")
    private String channelCode;

    @NotBlank(message = "类型不能为空")
    private String channelType;

    @JsonAlias({"h5Link", "h5LinkUrl"})
    private String h5Url;

    private Integer status;
    private String businessOwner;
    private Integer dailyQuota;
    private Integer normalRecommend;
    private Integer displayProductCount;
    private Integer actualPushCount;
    private String methodName;
    private String encryptType;
    private String cipherMode;
    private String paddingMode;
    private String ivValue;

    @NotBlank(message = "秘钥配置不能为空")
    private String appKey;

    private String ipWhitelist;
    private String callbackUrl;
    private String settlementMode;

    @NotNull(message = "单价或返点比例不能为空")
    @DecimalMin(value = "0", inclusive = true, message = "单价或返点比例不能小于0")
    private BigDecimal feeRate;

    @DecimalMin(value = "0", inclusive = true, message = "minPrice must be >= 0")
    private BigDecimal minPrice;

    @DecimalMin(value = "0", inclusive = true, message = "maxPrice must be >= 0")
    private BigDecimal maxPrice;

    private String priceReturnMode;

    private String extJson;
    private String remark;
}
