package com.jyfq.loan.service;

import com.jyfq.loan.model.dto.AdminMenuQueryDTO;
import com.jyfq.loan.model.dto.AdminMenuSaveDTO;
import com.jyfq.loan.model.vo.AdminMenuVO;
import com.jyfq.loan.model.vo.OptionVO;

import java.util.List;

/**
 * Admin menu management service.
 */
public interface AdminMenuService {

    List<AdminMenuVO> listMenus(AdminMenuQueryDTO query);

    AdminMenuVO getMenuDetail(Long id);

    Long createMenu(AdminMenuSaveDTO request);

    void updateMenu(Long id, AdminMenuSaveDTO request);

    void deleteMenu(Long id);

    void toggleMenu(Long id);

    List<OptionVO> listMenuOptions();
}
