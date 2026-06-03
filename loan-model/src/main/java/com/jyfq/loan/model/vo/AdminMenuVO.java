package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin menu tree node.
 */
@Data
public class AdminMenuVO implements Serializable {

    private Long id;

    private Long parentId;

    private String menuName;

    private String menuCode;

    private Integer menuType;

    private String menuTypeDesc;

    private String path;

    private String component;

    private String permission;

    private String icon;

    private Integer sort;

    private Integer visible;

    private String visibleDesc;

    private Integer status;

    private String statusDesc;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<AdminMenuVO> children = new ArrayList<>();
}
