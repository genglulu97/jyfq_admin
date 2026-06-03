package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * City recommendation resolved from client IP.
 */
@Data
public class H5IpCityVO implements Serializable {

    private String ip;
    private String provinceName;
    private String cityCode;
    private String cityName;
    private Boolean located;
    private String source;
}
