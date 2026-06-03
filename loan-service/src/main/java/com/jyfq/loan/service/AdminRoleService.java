package com.jyfq.loan.service;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.model.dto.AdminRoleMenuDTO;
import com.jyfq.loan.model.dto.AdminRoleQueryDTO;
import com.jyfq.loan.model.dto.AdminRoleSaveDTO;
import com.jyfq.loan.model.vo.AdminRoleVO;
import com.jyfq.loan.model.vo.OptionVO;

import java.util.List;

/**
 * Admin role management service.
 */
public interface AdminRoleService {

    PageResult<AdminRoleVO> pageRoles(AdminRoleQueryDTO query);

    AdminRoleVO getRoleDetail(Long id);

    Long createRole(AdminRoleSaveDTO request);

    void updateRole(Long id, AdminRoleSaveDTO request);

    void deleteRole(Long id);

    void toggleRole(Long id);

    void assignMenus(Long id, AdminRoleMenuDTO request);

    List<OptionVO> listRoleOptions();
}
