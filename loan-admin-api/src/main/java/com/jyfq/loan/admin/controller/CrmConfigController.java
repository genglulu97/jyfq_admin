package com.jyfq.loan.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.mapper.CrmEmployeeProfileMapper;
import com.jyfq.loan.mapper.CrmInstitutionConfigMapper;
import com.jyfq.loan.mapper.CrmRuleConfigMapper;
import com.jyfq.loan.mapper.CrmTeamMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.model.dto.CrmInstitutionConfigQueryDTO;
import com.jyfq.loan.model.dto.CrmInstitutionConfigSaveDTO;
import com.jyfq.loan.model.entity.CrmEmployeeProfile;
import com.jyfq.loan.model.entity.CrmInstitutionConfig;
import com.jyfq.loan.model.entity.CrmRuleConfig;
import com.jyfq.loan.model.entity.CrmTeam;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.vo.CrmInstitutionConfigVO;
import com.jyfq.loan.model.vo.OptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "CRM Config")
@RestController
@RequestMapping({"/admin/crm/config", "/admin/crm/binding"})
@RequiredArgsConstructor
public class CrmConfigController {

    private final CrmTeamMapper crmTeamMapper;
    private final CrmRuleConfigMapper crmRuleConfigMapper;
    private final CrmInstitutionConfigMapper crmInstitutionConfigMapper;
    private final CrmEmployeeProfileMapper crmEmployeeProfileMapper;
    private final InstitutionMapper institutionMapper;

    @Operation(summary = "team list")
    @GetMapping("/team/list")
    public R<PageResult<CrmTeam>> teamList(@RequestParam(required = false) String teamName,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) Long current,
                                           @RequestParam(required = false) Long size) {
        LambdaQueryWrapper<CrmTeam> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(teamName)) {
            wrapper.like(CrmTeam::getTeamName, teamName.trim());
        }
        if (status != null) {
            wrapper.eq(CrmTeam::getStatus, status);
        }
        wrapper.orderByDesc(CrmTeam::getCreatedAt).orderByDesc(CrmTeam::getId);
        Page<CrmTeam> page = crmTeamMapper.selectPage(new Page<>(pageCurrent(current), pageSize(size)), wrapper);
        return R.ok(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords()));
    }

    @Operation(summary = "save team")
    @PostMapping("/team/save")
    public R<Long> saveTeam(@RequestBody CrmTeam request) {
        if (request.getId() == null) {
            request.setStatus(request.getStatus() == null ? 1 : request.getStatus());
            crmTeamMapper.insert(request);
        } else {
            crmTeamMapper.updateById(request);
        }
        return R.ok(request.getId());
    }

    @Operation(summary = "toggle team")
    @PutMapping("/team/toggle/{id}")
    public R<Void> toggleTeam(@PathVariable Long id) {
        CrmTeam team = crmTeamMapper.selectById(id);
        if (team != null) {
            CrmTeam update = new CrmTeam();
            update.setId(id);
            update.setStatus(Integer.valueOf(1).equals(team.getStatus()) ? 0 : 1);
            crmTeamMapper.updateById(update);
        }
        return R.ok();
    }

    @Operation(summary = "rule list")
    @GetMapping("/rule/list")
    public R<List<CrmRuleConfig>> ruleList() {
        return R.ok(crmRuleConfigMapper.selectList(new LambdaQueryWrapper<CrmRuleConfig>()
                .orderByAsc(CrmRuleConfig::getId)));
    }

    @Operation(summary = "save rule")
    @PostMapping("/rule/save")
    public R<Long> saveRule(@RequestBody CrmRuleConfig request) {
        if (request.getId() == null) {
            request.setStatus(request.getStatus() == null ? 1 : request.getStatus());
            crmRuleConfigMapper.insert(request);
        } else {
            crmRuleConfigMapper.updateById(request);
        }
        return R.ok(request.getId());
    }

    @Operation(summary = "CRM institution config list")
    @GetMapping({"/institution/list", "/list"})
    public R<PageResult<CrmInstitutionConfigVO>> institutionConfigList(CrmInstitutionConfigQueryDTO query) {
        CrmInstitutionConfigQueryDTO safeQuery = query == null ? new CrmInstitutionConfigQueryDTO() : query;
        LambdaQueryWrapper<CrmInstitutionConfig> wrapper = new LambdaQueryWrapper<>();
        if (safeQuery.getInstId() != null) {
            wrapper.eq(CrmInstitutionConfig::getInstId, safeQuery.getInstId());
        }
        if (StringUtils.hasText(safeQuery.getInstCode())) {
            wrapper.like(CrmInstitutionConfig::getInstCode, safeQuery.getInstCode().trim());
        }
        if (StringUtils.hasText(safeQuery.getInstName())) {
            wrapper.like(CrmInstitutionConfig::getInstName, safeQuery.getInstName().trim());
        }
        if (safeQuery.getPlatformInstId() != null) {
            wrapper.eq(CrmInstitutionConfig::getPlatformInstId, safeQuery.getPlatformInstId());
        }
        if (StringUtils.hasText(safeQuery.getPlatformInstCode())) {
            wrapper.like(CrmInstitutionConfig::getPlatformInstCode, safeQuery.getPlatformInstCode().trim());
        }
        if (StringUtils.hasText(safeQuery.getPlatformInstName())) {
            wrapper.like(CrmInstitutionConfig::getPlatformInstName, safeQuery.getPlatformInstName().trim());
        }
        if (safeQuery.getCrmInstId() != null) {
            wrapper.eq(CrmInstitutionConfig::getCrmInstId, safeQuery.getCrmInstId());
        }
        if (StringUtils.hasText(safeQuery.getCrmInstCode())) {
            wrapper.like(CrmInstitutionConfig::getCrmInstCode, safeQuery.getCrmInstCode().trim());
        }
        if (StringUtils.hasText(safeQuery.getCrmInstName())) {
            wrapper.like(CrmInstitutionConfig::getCrmInstName, safeQuery.getCrmInstName().trim());
        }
        if (safeQuery.getStatus() != null) {
            wrapper.eq(CrmInstitutionConfig::getStatus, safeQuery.getStatus());
        }
        wrapper.orderByDesc(CrmInstitutionConfig::getUpdatedAt).orderByDesc(CrmInstitutionConfig::getId);
        Page<CrmInstitutionConfig> page = crmInstitutionConfigMapper.selectPage(
                new Page<>(pageCurrent(safeQuery.getCurrent()), pageSize(safeQuery.getSize())), wrapper);
        return R.ok(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(this::toConfigVO).toList()));
    }

    @Operation(summary = "CRM institution config detail")
    @GetMapping({"/institution/detail/{id}", "/detail/{id}", "/{id}"})
    public R<CrmInstitutionConfigVO> institutionConfigDetail(@PathVariable Long id) {
        CrmInstitutionConfig config = requireInstitutionConfig(id);
        return R.ok(toConfigVO(config));
    }

    @Operation(summary = "CRM institution options")
    @GetMapping({"/institution/options", "/options"})
    public R<List<OptionVO>> crmInstitutionOptions() {
        List<OptionVO> options = institutionMapper.selectList(new LambdaQueryWrapper<Institution>()
                        .eq(Institution::getStatus, 1)
                        .orderByDesc(Institution::getUpdatedAt)
                        .orderByDesc(Institution::getId))
                .stream()
                .map(institution -> new OptionVO(resolveInstitutionOptionLabel(institution), String.valueOf(institution.getId())))
                .toList();
        return R.ok(options);
    }

    @Operation(summary = "save CRM institution config")
    @PostMapping({"/institution/save", "/add"})
    public R<Long> saveInstitutionConfig(@RequestBody CrmInstitutionConfigSaveDTO request) {
        return saveInstitutionConfigInternal(request);
    }

    @Operation(summary = "update CRM institution config")
    @PutMapping({"/institution/update/{id}", "/update/{id}"})
    public R<Long> updateInstitutionConfig(@PathVariable Long id,
                                           @RequestBody CrmInstitutionConfigSaveDTO request) {
        request.setId(id);
        return saveInstitutionConfigInternal(request);
    }

    @Operation(summary = "delete CRM institution config")
    @DeleteMapping({"/institution/delete/{id}", "/delete/{id}"})
    public R<Void> deleteInstitutionConfig(@PathVariable Long id) {
        crmInstitutionConfigMapper.deleteById(id);
        return R.ok();
    }

    @Operation(summary = "check CRM institution config")
    @PostMapping({"/institution/check/{id}", "/check/{id}"})
    public R<CrmInstitutionConfigVO> checkInstitutionConfig(@PathVariable Long id) {
        return R.ok(toConfigVO(requireInstitutionConfig(id)));
    }

    @Operation(summary = "toggle CRM institution config status")
    @PostMapping({"/institution/toggle/{id}", "/toggle/{id}", "/institution/status/{id}", "/status/{id}"})
    public R<CrmInstitutionConfigVO> toggleInstitutionConfig(@PathVariable Long id) {
        CrmInstitutionConfig config = requireInstitutionConfig(id);
        CrmInstitutionConfig update = new CrmInstitutionConfig();
        update.setId(id);
        update.setStatus(Integer.valueOf(1).equals(config.getStatus()) ? 0 : 1);
        crmInstitutionConfigMapper.updateById(update);
        return R.ok(toConfigVO(crmInstitutionConfigMapper.selectById(id)));
    }

    private R<Long> saveInstitutionConfigInternal(CrmInstitutionConfigSaveDTO request) {
        Long crmInstId = request.getCrmInstId() == null ? request.getInstId() : request.getCrmInstId();
        if (crmInstId == null) {
            throw new BizException("crmInstId is required");
        }
        Institution crmInstitution = institutionMapper.selectById(crmInstId);
        if (crmInstitution == null) {
            throw new BizException("CRM institution not found: " + crmInstId);
        }
        Institution platformInstitution = null;
        if (request.getPlatformInstId() != null) {
            platformInstitution = institutionMapper.selectById(request.getPlatformInstId());
            if (platformInstitution == null) {
                throw new BizException("platform institution not found: " + request.getPlatformInstId());
            }
        }

        CrmInstitutionConfig config = request.getId() == null
                ? findConfigByInstId(crmInstId)
                : crmInstitutionConfigMapper.selectById(request.getId());
        if (config == null) {
            config = new CrmInstitutionConfig();
        }
        config.setInstId(crmInstitution.getId());
        config.setInstCode(crmInstitution.getInstCode());
        config.setInstName(crmInstitution.getInstName());
        config.setCrmInstId(crmInstitution.getId());
        config.setCrmInstCode(crmInstitution.getInstCode());
        config.setCrmInstName(crmInstitution.getInstName());
        config.setCrmOrgId(trimToNull(request.getCrmOrgId()));
        config.setCrmOrgCode(trimToNull(request.getCrmOrgCode()));
        config.setCrmOrgName(trimToNull(request.getCrmOrgName()));
        if (platformInstitution != null) {
            config.setPlatformInstId(platformInstitution.getId());
            config.setPlatformInstCode(platformInstitution.getInstCode());
            config.setPlatformInstName(platformInstitution.getInstName());
        } else {
            config.setPlatformInstId(null);
            config.setPlatformInstCode(null);
            config.setPlatformInstName(null);
        }
        config.setAutoPush(request.getAutoPush() == null ? 1 : request.getAutoPush());
        config.setAutoAssign(request.getAutoAssign() == null ? 0 : request.getAutoAssign());
        config.setOwnerAdminId(request.getOwnerAdminId());
        config.setCrmAdminName(trimToNull(request.getCrmAdminName()));
        config.setCrmAdminPhone(trimToNull(request.getCrmAdminPhone()));
        config.setCrmAdminEmail(trimToNull(request.getCrmAdminEmail()));
        config.setCrmAdminRole(trimToNull(request.getCrmAdminRole()));
        config.setCrmAdminAccount(trimToNull(request.getCrmAdminAccount()));
        config.setTeamId(request.getTeamId());
        config.setCustomerSource(StringUtils.hasText(request.getCustomerSource()) ? request.getCustomerSource().trim() : "CRM_API");
        config.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        config.setRemark(request.getRemark());
        if (request.getOwnerAdminId() != null) {
            CrmEmployeeProfile profile = crmEmployeeProfileMapper.selectOne(new LambdaQueryWrapper<CrmEmployeeProfile>()
                    .eq(CrmEmployeeProfile::getAdminId, request.getOwnerAdminId())
                    .last("LIMIT 1"));
            if (profile != null) {
                config.setOwnerName(profile.getEmployeeName());
                if (config.getTeamId() == null) {
                    config.setTeamId(profile.getTeamId());
                }
            }
        } else {
            config.setOwnerName(null);
        }
        if (config.getId() == null) {
            crmInstitutionConfigMapper.insert(config);
        } else {
            crmInstitutionConfigMapper.updateById(config);
        }
        return R.ok(config.getId());
    }

    private CrmInstitutionConfig requireInstitutionConfig(Long id) {
        CrmInstitutionConfig config = crmInstitutionConfigMapper.selectById(id);
        if (config == null) {
            throw new BizException("CRM institution config not found: " + id);
        }
        return config;
    }

    private CrmInstitutionConfig findConfigByInstId(Long instId) {
        return crmInstitutionConfigMapper.selectOne(new LambdaQueryWrapper<CrmInstitutionConfig>()
                .and(wrapper -> wrapper.eq(CrmInstitutionConfig::getInstId, instId)
                        .or()
                        .eq(CrmInstitutionConfig::getCrmInstId, instId))
                .last("LIMIT 1"));
    }

    private CrmInstitutionConfigVO toConfigVO(CrmInstitutionConfig config) {
        CrmInstitutionConfigVO vo = new CrmInstitutionConfigVO();
        vo.setId(config.getId());
        vo.setInstId(config.getInstId());
        vo.setInstCode(config.getInstCode());
        vo.setInstName(config.getInstName());
        vo.setPlatformInstId(config.getPlatformInstId());
        vo.setPlatformInstCode(config.getPlatformInstCode());
        vo.setPlatformInstName(config.getPlatformInstName());
        vo.setCrmInstId(config.getCrmInstId());
        vo.setCrmInstCode(config.getCrmInstCode());
        vo.setCrmInstName(config.getCrmInstName());
        vo.setCrmOrgId(config.getCrmOrgId());
        vo.setCrmOrgCode(config.getCrmOrgCode());
        vo.setCrmOrgName(config.getCrmOrgName());
        vo.setAutoPush(config.getAutoPush());
        vo.setAutoAssign(config.getAutoAssign());
        vo.setOwnerAdminId(config.getOwnerAdminId());
        vo.setOwnerName(config.getOwnerName());
        vo.setCrmAdminName(config.getCrmAdminName());
        vo.setCrmAdminPhone(config.getCrmAdminPhone());
        vo.setCrmAdminEmail(config.getCrmAdminEmail());
        vo.setCrmAdminRole(config.getCrmAdminRole());
        vo.setCrmAdminAccount(config.getCrmAdminAccount());
        vo.setTeamId(config.getTeamId());
        vo.setCustomerSource(config.getCustomerSource());
        vo.setStatus(config.getStatus());
        vo.setRemark(config.getRemark());
        vo.setCreatedAt(config.getCreatedAt());
        vo.setUpdatedAt(config.getUpdatedAt());
        return vo;
    }

    private String resolveInstitutionOptionLabel(Institution institution) {
        if (institution == null) {
            return "";
        }
        if (StringUtils.hasText(institution.getMerchantAlias())) {
            return institution.getMerchantAlias().trim();
        }
        if (StringUtils.hasText(institution.getInstName())) {
            return institution.getInstName().trim();
        }
        return institution.getInstCode();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private long pageCurrent(Long current) {
        return current == null || current < 1 ? 1L : current;
    }

    private long pageSize(Long size) {
        return size == null || size < 1 ? 10L : Math.min(size, 200L);
    }
}
