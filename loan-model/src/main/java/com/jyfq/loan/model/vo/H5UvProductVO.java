package com.jyfq.loan.model.vo;

import lombok.Data;

/**
 * UV product view for H5 page.
 */
@Data
public class H5UvProductVO {

    private Long id;
    private String name;
    private String logo;
    private String loanType;
    private Integer minAmount;
    private Integer maxAmount;
    private String rate;
    private String term;
    private String badge;
    private String applyUrl;
    private Integer weight;
}
