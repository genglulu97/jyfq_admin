package com.jyfq.loan.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Admin menu create/update request.
 */
@Data
public class AdminMenuSaveDTO implements Serializable {

    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 64, message = "菜单名称长度不能超过64")
    private String menuName;

    @NotBlank(message = "菜单编码不能为空")
    @Size(max = 64, message = "菜单编码长度不能超过64")
    private String menuCode;

    @Min(value = 1, message = "菜单类型只能是1、2、3")
    @Max(value = 3, message = "菜单类型只能是1、2、3")
    private Integer menuType;

    @Size(max = 255, message = "路由地址长度不能超过255")
    private String path;

    @Size(max = 255, message = "组件路径长度不能超过255")
    private String component;

    @Size(max = 128, message = "权限标识长度不能超过128")
    private String permission;

    @Size(max = 64, message = "图标长度不能超过64")
    private String icon;

    private Integer sort;

    @Min(value = 0, message = "显示状态只能是0或1")
    @Max(value = 1, message = "显示状态只能是0或1")
    private Integer visible;

    @Min(value = 0, message = "状态只能是0或1")
    @Max(value = 1, message = "状态只能是0或1")
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
