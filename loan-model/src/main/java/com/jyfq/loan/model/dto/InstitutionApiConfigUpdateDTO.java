package com.jyfq.loan.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serializable;

/**
 * Institution API config update request.
 */
@Data
public class InstitutionApiConfigUpdateDTO implements Serializable {

    @JsonAlias({"apiMethodName", "adapterBeanName"})
    private String beanName;

    private String businessCode;

    @JsonAlias({"checkUrl", "apiCheckUrl", "collisionUrl"})
    private String preCheckUrl;

    @JsonAlias({"pushUrl", "applyUrl"})
    private String apiPushUrl;

    private String apiNotifyUrl;

    @JsonAlias({"apiKey", "partnerKey"})
    private String appKey;

    private String encryptType;
    private String cipherMode;
    private String paddingMode;
    private String ivValue;

    private String notifyEncryptType;
    private String notifyCipherMode;
    private String notifyPaddingMode;
    private String notifyIvValue;

    private Integer timeoutMs;
}
