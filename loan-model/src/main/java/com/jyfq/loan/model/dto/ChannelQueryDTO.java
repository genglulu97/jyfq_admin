package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Admin channel query params.
 */
@Data
public class ChannelQueryDTO implements Serializable {

    private Long current = 1L;

    private Long size = 10L;

    private String channelName;

    private String channelCode;

    private Integer status;
}
