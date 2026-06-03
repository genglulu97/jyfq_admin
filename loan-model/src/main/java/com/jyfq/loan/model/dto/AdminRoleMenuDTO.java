package com.jyfq.loan.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Role menu assignment request.
 */
@Data
public class AdminRoleMenuDTO implements Serializable {

    private List<Long> menuIds;
}
