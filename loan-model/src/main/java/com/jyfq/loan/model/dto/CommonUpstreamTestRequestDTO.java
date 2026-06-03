package com.jyfq.loan.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * Plain request for frontend performance testing.
 */
@Data
public class CommonUpstreamTestRequestDTO implements Serializable {

    @JsonAlias({"channelCode"})
    private String orgCode;

    @JsonAlias({"userName"})
    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "phone is required")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone format is invalid")
    private String phone;

    @JsonAlias({"idNo", "idCardNo", "certNo"})
    @NotBlank(message = "idCard is required")
    @Pattern(regexp = "^(\\d{15}|\\d{17}[0-9Xx])$", message = "idCard format is invalid")
    private String idCard;

    @JsonAlias({"workCity"})
    @NotBlank(message = "city is required")
    private String city;

    private String cityCode;
    private String collisionNo;
    private String localOrderNo;
    private Long productId;
}
