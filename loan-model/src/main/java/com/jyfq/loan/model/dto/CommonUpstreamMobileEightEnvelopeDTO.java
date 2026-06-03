package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 8-digit masked mobile upstream encrypted envelope.
 */
@Data
public class CommonUpstreamMobileEightEnvelopeDTO implements Serializable {

    @NotBlank(message = "channelCode is required")
    private String channelCode;

    @NotBlank(message = "data is required")
    private String data;
}
