package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * H5 promotion tracking event.
 */
@Data
public class H5TrackDTO implements Serializable {

    @NotBlank(message = "channelCode cannot be empty")
    private String channelCode;

    /** PV, CLICK, REGISTER, COMPLETE. */
    @NotBlank(message = "eventType cannot be empty")
    private String eventType;

    private String visitorId;
    private String sessionId;
    private String pageUrl;
    private String referer;
    private String extJson;
}
