package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * Institution status update request.
 */
@Data
public class InstitutionStatusUpdateDTO implements Serializable {

    @NotNull(message = "status is required")
    private Integer status;
}
