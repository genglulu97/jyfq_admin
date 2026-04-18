package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Institution API config detail view.
 */
@Data
public class InstitutionApiConfigDetailVO implements Serializable {

    private Long id;
    private String instCode;
    private String instName;
    private String merchantAlias;
    private String merchantType;
    private String beanName;
    private String businessCode;
    private String preCheckUrl;
    private String apiPushUrl;
    private String apiNotifyUrl;
    private String appKey;
    private String encryptType;
    private String notifyEncryptType;
    private Integer timeoutMs;
    private Integer status;
    private String statusDesc;
    private LocalDateTime createdAt;
    private String createBy;
    private LocalDateTime updatedAt;
    private String updateBy;
}
