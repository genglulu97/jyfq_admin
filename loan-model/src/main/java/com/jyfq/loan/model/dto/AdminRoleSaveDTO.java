package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Admin role create/update request.
 */
@Data
public class AdminRoleSaveDTO implements Serializable {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 32, message = "角色名称长度不能超过32")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 32, message = "角色编码长度不能超过32")
    private String roleCode;

    @Size(max = 255, message = "角色说明长度不能超过255")
    private String description;

    @Min(value = 0, message = "状态只能是0或1")
    @Max(value = 1, message = "状态只能是0或1")
    private Integer status;

    private Integer sort;

    private List<Long> menuIds;
}
