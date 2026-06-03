package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Hourly report query parameters.
 */
@Data
public class HourlyReportQueryDTO implements Serializable {

    private Long current = 1L;

    private Long size = 20L;

    /** yyyy-MM-dd or yyyy-MM-dd HH:mm:ss. */
    private String startDate;

    /** yyyy-MM-dd or yyyy-MM-dd HH:mm:ss. */
    private String endDate;

    /** Backward-compatible single day query. */
    private String date;

    private Long channelId;

    private String channelCode;

    private Long instId;
}
