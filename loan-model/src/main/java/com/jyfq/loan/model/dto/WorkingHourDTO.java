package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Working hour rule for institution product routing.
 */
@Data
public class WorkingHourDTO implements Serializable {

    private String dayOfWeek;
    private String startTime;
    private String endTime;
}
