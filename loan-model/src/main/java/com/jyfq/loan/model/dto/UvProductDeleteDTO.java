package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * UV product delete request.
 */
@Data
public class UvProductDeleteDTO {

    @NotEmpty(message = "ids is required")
    private List<Long> ids;
}
