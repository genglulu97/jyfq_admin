package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Admin institution report query parameters.
 */
@Data
public class InstitutionReportQueryDTO implements Serializable {

    private Long current = 1L;

    private Long size = 20L;

    /**
     * yyyy-MM-dd or yyyy-MM-dd HH:mm:ss.
     */
    private String startDate;

    /**
     * yyyy-MM-dd or yyyy-MM-dd HH:mm:ss.
     */
    private String endDate;

    private Long instId;

    /**
     * Reserved for frontend filter values such as day/month.
     */
    private String statGranularity = "day";

    private String statisticGranularity;

    private String granularity;
}
