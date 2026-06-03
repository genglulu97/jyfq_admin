package com.jyfq.loan.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * H5 authorization confirmation and formal application request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class H5ApplyConfirmRequestDTO extends H5ApplyRequestDTO {

    @JsonAlias({"userName"})
    @NotBlank(message = "name is required")
    private String name;

    @JsonAlias({"idNo", "idCardNo"})
    @NotBlank(message = "idCard is required")
    @Pattern(regexp = "^(\\d{15}|\\d{17}[0-9Xx])$", message = "idCard format is invalid")
    private String idCard;

    @JsonAlias({"collisionNo"})
    @NotBlank(message = "localOrderNo is required")
    private String localOrderNo;

    private Long productId;
}
