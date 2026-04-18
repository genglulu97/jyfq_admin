package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * Common upstream encrypted envelope.
 */
@Data
public class CommonUpstreamEnvelopeDTO implements Serializable {

    @NotBlank(message = "orgCode is required")
    private String orgCode;

    @NotBlank(message = "data is required")
    private String data;
}
