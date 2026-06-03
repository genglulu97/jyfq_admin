package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Admin user password reset request.
 */
@Data
public class AdminUserPasswordDTO implements Serializable {

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度必须在6到64之间")
    private String password;
}
