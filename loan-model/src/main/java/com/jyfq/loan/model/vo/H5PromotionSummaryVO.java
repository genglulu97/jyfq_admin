package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * H5 promotion summary metrics.
 */
@Data
public class H5PromotionSummaryVO implements Serializable {

    private Long channelCount;
    private Long pvCount;
    private Long clickCount;
    private Long registerCount;
    private Long completeCount;
    private String clickRate;
    private String registerConversionRate;
    private String completeConversionRate;
}
