package com.jyfq.loan.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * UV product management view.
 */
@Data
public class UvProductVO {

    private Long id;
    private String name;
    private String logo;
    private String status;
    private String position;
    private String loanType;
    private Integer minAmount;
    private Integer maxAmount;
    private String rate;
    private String term;
    private Integer weight;
    private BigDecimal price;
    private Integer uvThreshold;
    private String badge;
    private String isJoint;
    private String applyUrl;
    private String jointChannel;
    private String jointKey;
    private String jointCheckUrl;
    private String jointLoginUrl;
    private String jointRegAgreement;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime autoTimeStart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime autoTimeEnd;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime autoOfflineTime;

    private String assocInst;
    private String specChannels;
    private Integer minAge;
    private Integer maxAge;
    private String blockProvinces;
    private String blockCities;
    private String targetRegions;
    private Integer applyCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    private List<String> zhima;
    private List<String> house;
    private List<String> car;
    private List<String> gongjijin;
    private List<String> job;
}
