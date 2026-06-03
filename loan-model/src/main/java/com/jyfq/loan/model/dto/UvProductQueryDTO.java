package com.jyfq.loan.model.dto;

import lombok.Data;

/**
 * UV product list query.
 */
@Data
public class UvProductQueryDTO {

    private Integer pageNum;
    private Integer pageSize;
    private String name;
    private String status;
    private String isJoint;
    private String position;
    private String createTimeStart;
    private String createTimeEnd;
}
