package com.jyfq.loan.service;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.model.dto.AdminUserPasswordDTO;
import com.jyfq.loan.model.dto.AdminUserQueryDTO;
import com.jyfq.loan.model.dto.AdminUserSaveDTO;
import com.jyfq.loan.model.vo.AdminUserVO;
import com.jyfq.loan.model.vo.OptionVO;

import java.util.List;

/**
 * Admin user management service.
 */
public interface AdminUserService {

    PageResult<AdminUserVO> pageUsers(AdminUserQueryDTO query);

    AdminUserVO getUserDetail(Long id);

    Long createUser(AdminUserSaveDTO request);

    void updateUser(Long id, AdminUserSaveDTO request);

    void deleteUser(Long id);

    void toggleUser(Long id);

    void resetPassword(Long id, AdminUserPasswordDTO request);

    List<OptionVO> listRoleOptions();
}
