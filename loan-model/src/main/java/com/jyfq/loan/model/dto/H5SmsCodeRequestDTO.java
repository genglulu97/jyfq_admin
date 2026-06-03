package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * H5 SMS verification code request.
 */
@Data
public class H5SmsCodeRequestDTO implements Serializable {

    @NotBlank(message = "channelCode is required")
    private String channelCode;

    @NotBlank(message = "phone is required")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone format is invalid")
    private String phone;
}
