package com.jyfq.loan.admin.support;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.mapper.CrmEmployeeProfileMapper;
import com.jyfq.loan.mapper.SysAdminMapper;
import com.jyfq.loan.model.dto.CrmCurrentUserDTO;
import com.jyfq.loan.model.entity.CrmEmployeeProfile;
import com.jyfq.loan.model.entity.SysAdmin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CrmCurrentUserResolver {

    private final SysAdminMapper sysAdminMapper;
    private final CrmEmployeeProfileMapper crmEmployeeProfileMapper;

    public CrmCurrentUserDTO resolve() {
        if (!StpUtil.isLogin()) {
            throw new BizException(2001, "not logged in");
        }
        String loginId = String.valueOf(StpUtil.getLoginId());
        Long adminId = parseAdminId(loginId);
        CrmCurrentUserDTO user = new CrmCurrentUserDTO();
        user.setAdminId(adminId);
        if (adminId == null) {
            user.setUsername("admin");
            user.setRealName("admin");
            user.setRole("SUPER_ADMIN");
            return user;
        }
        SysAdmin admin = sysAdminMapper.selectById(adminId);
        if (admin == null) {
            throw new BizException(2001, "login user not found");
        }
        user.setUsername(admin.getUsername());
        user.setRealName(StringUtils.hasText(admin.getRealName()) ? admin.getRealName() : admin.getUsername());
        user.setRole(admin.getRole());
        CrmEmployeeProfile profile = crmEmployeeProfileMapper.selectOne(new LambdaQueryWrapper<CrmEmployeeProfile>()
                .eq(CrmEmployeeProfile::getAdminId, adminId)
                .last("LIMIT 1"));
        if (profile != null) {
            user.setTeamId(profile.getTeamId());
            if (StringUtils.hasText(profile.getCrmRole())) {
                user.setRole(profile.getCrmRole().trim().toUpperCase());
            }
            if (StringUtils.hasText(profile.getEmployeeName())) {
                user.setRealName(profile.getEmployeeName());
            }
        }
        return user;
    }

    private Long parseAdminId(String loginId) {
        if (!StringUtils.hasText(loginId) || !loginId.startsWith("admin:")) {
            return null;
        }
        String idPart = loginId.substring("admin:".length());
        if ("admin".equals(idPart)) {
            return null;
        }
        try {
            return Long.valueOf(idPart);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
