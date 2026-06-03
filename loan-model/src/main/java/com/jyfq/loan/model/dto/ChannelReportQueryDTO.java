package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Channel report query parameters.
 */
@Data
public class ChannelReportQueryDTO implements Serializable {

    private Long current = 1L;

    private Long size = 20L;

    /** yyyy-MM-dd or yyyy-MM-dd HH:mm:ss. */
    private String startDate;

    /** yyyy-MM-dd or yyyy-MM-dd HH:mm:ss. */
    private String endDate;

    private String channelCode;

    /** DAY or SUMMARY. Aliases: granularity, groupBy. */
    private String statisticGranularity = "DAY";

    private String granularity;

    private String groupBy;
}
