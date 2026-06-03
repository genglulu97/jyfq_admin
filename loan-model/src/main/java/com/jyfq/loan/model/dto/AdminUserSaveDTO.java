package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Admin user create/update request.
 */
@Data
public class AdminUserSaveDTO implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名长度不能超过32")
    private String username;

    @Size(max = 64, message = "密码长度不能超过64")
    private String password;

    @Size(max = 32, message = "姓名长度不能超过32")
    private String realName;

    @Size(max = 32, message = "角色长度不能超过32")
    private String role;

    @Min(value = 0, message = "状态只能是0或1")
    @Max(value = 1, message = "状态只能是0或1")
    private Integer status;
}
