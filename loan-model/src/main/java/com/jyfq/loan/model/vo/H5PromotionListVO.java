package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * H5 promotion list item.
 */
@Data
public class H5PromotionListVO implements Serializable {

    private Long id;
    private String channelName;
    private String channelCode;
    private String promoteLink;
    private Integer status;
    private String statusDesc;
    private Long pvCount;
    private Long clickCount;
    private String clickRate;
    private Long registerCount;
    private Long completeCount;
    private String registerConversionRate;
    private String completeConversionRate;
    private LocalDateTime updatedAt;
}
