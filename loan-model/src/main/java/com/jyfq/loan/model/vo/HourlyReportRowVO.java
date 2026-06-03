package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Hourly report table row.
 */
@Data
public class HourlyReportRowVO implements Serializable {

    private String hour;

    private String statHour;

    private Long totalOrders;

    private Long successOrders;

    private String successRate;
}
