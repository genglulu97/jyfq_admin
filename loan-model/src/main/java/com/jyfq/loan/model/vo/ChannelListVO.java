package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Channel list item.
 */
@Data
public class ChannelListVO implements Serializable {

    private Long id;
    private String channelId;
    private String channelName;
    private String channelCode;
    private String channelType;
    private Integer status;
    private String statusDesc;
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
    private String appKey;
    private String ipWhitelist;
    private String callbackUrl;
    private String settlementMode;
    private BigDecimal feeRate;
    private String extJson;
    private String remark;
    private LocalDateTime createdAt;
    private String createBy;
    private LocalDateTime updatedAt;
    private String updateBy;
}
