package com.jyfq.loan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.common.util.AuditOperatorUtil;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.mapper.CityConfigMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.mapper.InstitutionProductMapper;
import com.jyfq.loan.mapper.InstitutionRechargeRecordMapper;
import com.jyfq.loan.model.dto.InstitutionQueryDTO;
import com.jyfq.loan.model.dto.InstitutionRechargeDTO;
import com.jyfq.loan.model.dto.InstitutionSaveDTO;
import com.jyfq.loan.model.entity.CityConfig;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.entity.InstitutionProduct;
import com.jyfq.loan.model.entity.InstitutionRechargeRecord;
import com.jyfq.loan.model.vo.InstitutionListVO;
import com.jyfq.loan.model.vo.InstitutionProductVO;
import com.jyfq.loan.model.vo.InstitutionRechargeRecordVO;
import com.jyfq.loan.model.vo.OptionVO;
import com.jyfq.loan.service.AdminInstitutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Admin institution management service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInstitutionServiceImpl implements AdminInstitutionService {

    private final CityConfigMapper cityConfigMapper;
    private final InstitutionMapper institutionMapper;
    private final InstitutionProductMapper institutionProductMapper;
    private final InstitutionRechargeRecordMapper rechargeRecordMapper;

    @Override
    public PageResult<InstitutionListVO> pageInstitutions(InstitutionQueryDTO query) {
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10L : Math.min(query.getSize(), 100L);

        LambdaQueryWrapper<Institution> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getMerchantType())) {
            wrapper.eq(Institution::getMerchantType, query.getMerchantType().trim());
        }
        if (StringUtils.hasText(query.getMerchantAlias())) {
            wrapper.like(Institution::getMerchantAlias, query.getMerchantAlias().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Institution::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Institution::getCreatedAt);

        Page<Institution> page = institutionMapper.selectPage(new Page<>(current, size), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResult.empty(current, size);
        }

        List<Long> instIds = page.getRecords().stream().map(Institution::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Long, List<InstitutionProduct>> productMap = institutionProductMapper.selectList(new LambdaQueryWrapper<InstitutionProduct>()
                        .in(InstitutionProduct::getInstId, instIds)
                        .orderByAsc(InstitutionProduct::getCreatedAt)
                        .orderByAsc(InstitutionProduct::getId))
                .stream()
                .collect(Collectors.groupingBy(InstitutionProduct::getInstId));

        List<InstitutionListVO> records = page.getRecords().stream().map(inst -> {
            InstitutionListVO vo = new InstitutionListVO();
            vo.setId(inst.getId());
            vo.setInstCode(inst.getInstCode());
            vo.setInstName(inst.getInstName());
            vo.setMerchantAlias(defaultText(inst.getMerchantAlias(), inst.getInstName()));
            List<InstitutionProduct> products = productMap.getOrDefault(inst.getId(), Collections.emptyList());
            vo.setProductName(joinProductNames(products));
            vo.setMerchantType(defaultText(inst.getMerchantType(), "机构"));
            vo.setOpenCities(joinProductCities(products));
            vo.setStatus(inst.getStatus());
            vo.setStatusDesc(resolveStatus(inst.getStatus()));
            vo.setAccountBalance(inst.getAccountBalance());
            vo.setCreatedAt(inst.getCreatedAt());
            vo.setCreateBy(inst.getCreateBy());
            vo.setUpdatedAt(inst.getUpdatedAt());
            vo.setUpdateBy(inst.getUpdateBy());
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInstitution(InstitutionSaveDTO request) {
        String instCode = generateInstCode(request.getMerchantAlias());

        Institution institution = new Institution();
        institution.setInstCode(instCode);
        institution.setInstName(request.getInstName().trim());
        institution.setMerchantAlias(request.getMerchantAlias().trim());
        institution.setMerchantType(defaultText(request.getMerchantType(), "机构"));
        institution.setApiPushUrl("http://127.0.0.1/mock/" + instCode + "/push");
        institution.setApiNotifyUrl("http://127.0.0.1:8082/loan-app/api/notify/" + instCode);
        institution.setAppKey("1234567890abcdef");
        institution.setEncryptType("AES");
        institution.setNotifyEncryptType("PLAIN");
        institution.setTimeoutMs(3000);
        institution.setStatus(request.getBusinessStatus() == null ? 1 : request.getBusinessStatus());
        institution.setAdminPhone(request.getAdminPhone());
        institution.setAdminName(request.getAdminName());
        institution.setAdminRole(defaultText(request.getAdminRole(), "管理员"));
        institution.setSmsNotify(defaultZeroOne(request.getSmsNotify(), 0));
        institution.setUserStatus(defaultZeroOne(request.getUserStatus(), 1));
        institution.setCrmAutoAssign(defaultZeroOne(request.getCrmAutoAssign(), 0));
        institution.setApiMerchant(defaultZeroOne(request.getApiMerchant(), 1));
        institution.setApiMethodName(request.getApiMethodName());
        institution.setSpecifiedChannel(request.getSpecifiedChannel());
        institution.setExcludedChannels(request.getExcludedChannels());
        institution.setAccountBalance(BigDecimal.ZERO);
        institution.setRechargeTotal(BigDecimal.ZERO);
        institution.setRemark(request.getRemark());
        institutionMapper.insert(institution);

        return institution.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInstitution(Long instId) {
        Institution institution = institutionMapper.selectById(instId);
        if (institution == null) {
            throw new BizException("商户不存在: " + instId);
        }

        long productCount = institutionProductMapper.selectCount(new LambdaQueryWrapper<InstitutionProduct>()
                .eq(InstitutionProduct::getInstId, instId));
        if (productCount > 0) {
            throw new BizException("当前商户下存在机构产品，请先删除机构产品配置");
        }

        long rechargeCount = rechargeRecordMapper.selectCount(new LambdaQueryWrapper<InstitutionRechargeRecord>()
                .eq(InstitutionRechargeRecord::getInstId, instId));
        if (rechargeCount > 0) {
            throw new BizException("当前商户已有充值记录，不能删除");
        }

        institutionMapper.deleteById(instId);
    }

    @Override
    public List<OptionVO> listCityOptions() {
        return cityConfigMapper.selectList(new LambdaQueryWrapper<CityConfig>()
                        .eq(CityConfig::getStatus, 1)
                        .orderByAsc(CityConfig::getSort)
                        .orderByAsc(CityConfig::getCityCode))
                .stream()
                .map(city -> new OptionVO(city.getCityName(), city.getCityCode()))
                .collect(Collectors.toList());
    }

    @Override
    public List<InstitutionProductVO> listProducts(Long instId) {
        List<InstitutionProduct> products = institutionProductMapper.selectList(new LambdaQueryWrapper<InstitutionProduct>()
                .eq(InstitutionProduct::getInstId, instId)
                .orderByDesc(InstitutionProduct::getCreatedAt)
                .orderByDesc(InstitutionProduct::getId));
        if (products.isEmpty()) {
            return Collections.emptyList();
        }

        return products.stream().map(product -> {
            InstitutionProductVO vo = new InstitutionProductVO();
            vo.setId(product.getId());
            vo.setInstId(product.getInstId());
            vo.setProductName(product.getProductName());
            vo.setProductIcon(product.getProductIcon());
            vo.setMaxAmount(product.getMaxAmount());
            vo.setRate(product.getRate());
            vo.setPeriod(product.getPeriod());
            vo.setProtocolUrl(product.getProtocolUrl());
            vo.setStatus(product.getStatus());
            vo.setStatusDesc(resolveStatus(product.getStatus()));
            vo.setCreatedAt(product.getCreatedAt());
            vo.setCreateBy(product.getCreateBy());
            vo.setUpdatedAt(product.getUpdatedAt());
            vo.setUpdateBy(product.getUpdateBy());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<InstitutionRechargeRecordVO> listRechargeRecords(Long instId) {
        List<InstitutionRechargeRecord> records = rechargeRecordMapper.selectList(new LambdaQueryWrapper<InstitutionRechargeRecord>()
                .eq(InstitutionRechargeRecord::getInstId, instId)
                .orderByDesc(InstitutionRechargeRecord::getRechargeTime)
                .orderByDesc(InstitutionRechargeRecord::getCreatedAt));
        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        return records.stream().map(record -> {
            InstitutionRechargeRecordVO vo = new InstitutionRechargeRecordVO();
            vo.setId(record.getId());
            vo.setInstId(record.getInstId());
            vo.setMerchantAlias(record.getMerchantAlias());
            vo.setOperatorName(record.getOperatorName());
            vo.setAmount(record.getAmount());
            vo.setRemark(record.getRemark());
            vo.setRechargeTime(record.getRechargeTime());
            vo.setCreatedAt(record.getCreatedAt());
            vo.setCreateBy(record.getCreateBy());
            vo.setUpdatedAt(record.getUpdatedAt());
            vo.setUpdateBy(record.getUpdateBy());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleInstitution(Long instId) {
        Institution institution = institutionMapper.selectById(instId);
        if (institution == null) {
            throw new BizException("鍟嗘埛涓嶅瓨鍦? " + instId);
        }

        int targetStatus = Integer.valueOf(1).equals(institution.getStatus()) ? 0 : 1;
        institutionMapper.update(null, new LambdaUpdateWrapper<Institution>()
                .eq(Institution::getId, instId)
                .set(Institution::getStatus, targetStatus)
                .set(Institution::getUpdateBy, AuditOperatorUtil.currentOperator()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long instId, InstitutionRechargeDTO request) {
        Institution institution = institutionMapper.selectById(instId);
        if (institution == null) {
            throw new BizException("商户不存在: " + instId);
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("充值金额必须大于0");
        }

        InstitutionRechargeRecord record = new InstitutionRechargeRecord();
        record.setInstId(instId);
        record.setInstCode(institution.getInstCode());
        record.setMerchantAlias(defaultText(institution.getMerchantAlias(), institution.getInstName()));
        record.setOperatorName(defaultText(request.getOperatorName(), "系统充值"));
        record.setAmount(amount);
        record.setRemark(request.getRemark());
        record.setRechargeTime(LocalDateTime.now());
        rechargeRecordMapper.insert(record);

        BigDecimal newBalance = institution.getAccountBalance() == null ? amount : institution.getAccountBalance().add(amount);
        BigDecimal newRechargeTotal = institution.getRechargeTotal() == null ? amount : institution.getRechargeTotal().add(amount);
        institutionMapper.update(null, new LambdaUpdateWrapper<Institution>()
                .eq(Institution::getId, instId)
                .set(Institution::getAccountBalance, newBalance)
                .set(Institution::getRechargeTotal, newRechargeTotal)
                .set(Institution::getUpdateBy, AuditOperatorUtil.currentOperator()));
    }

    private String generateInstCode(String merchantAlias) {
        String normalized = merchantAlias == null ? "merchant" : merchantAlias.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (!StringUtils.hasText(normalized)) {
            normalized = "merchant";
        }
        return normalized + "_" + System.currentTimeMillis();
    }

    private Integer defaultZeroOne(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String resolveStatus(Integer status) {
        if (status == null) {
            return "-";
        }
        return status == 1 ? "启用" : "禁用";
    }

    private String joinProductNames(List<InstitutionProduct> products) {
        if (products == null || products.isEmpty()) {
            return "-";
        }
        String joined = products.stream()
                .map(InstitutionProduct::getProductName)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(","));
        return StringUtils.hasText(joined) ? joined : "-";
    }

    private String joinProductCities(List<InstitutionProduct> products) {
        if (products == null || products.isEmpty()) {
            return "-";
        }
        Set<String> citySet = new LinkedHashSet<>();
        products.stream()
                .map(InstitutionProduct::getCityNames)
                .filter(StringUtils::hasText)
                .forEach(cityNames -> {
                    for (String city : cityNames.split("[,，;；\\n]")) {
                        String trimmed = city == null ? null : city.trim();
                        if (StringUtils.hasText(trimmed)) {
                            citySet.add(trimmed);
                        }
                    }
                });
        return citySet.isEmpty() ? "-" : String.join(",", citySet);
    }
}
