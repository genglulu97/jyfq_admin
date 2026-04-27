package com.jyfq.loan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.util.AuditOperatorUtil;
import com.jyfq.loan.mapper.CityConfigMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.mapper.InstitutionProductMapper;
import com.jyfq.loan.mapper.InstitutionRechargeRecordMapper;
import com.jyfq.loan.model.dto.InstitutionApiConfigUpdateDTO;
import com.jyfq.loan.model.dto.InstitutionQueryDTO;
import com.jyfq.loan.model.dto.InstitutionRechargeDTO;
import com.jyfq.loan.model.dto.InstitutionSaveDTO;
import com.jyfq.loan.model.dto.InstitutionStatusUpdateDTO;
import com.jyfq.loan.model.entity.CityConfig;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.entity.InstitutionProduct;
import com.jyfq.loan.model.entity.InstitutionRechargeRecord;
import com.jyfq.loan.model.vo.InstitutionApiConfigDetailVO;
import com.jyfq.loan.model.vo.InstitutionApiConfigListVO;
import com.jyfq.loan.model.vo.InstitutionApiConfigOptionsVO;
import com.jyfq.loan.model.vo.InstitutionDetailVO;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
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

    private static final String DEFAULT_ADAPTER_KEY = "qlqMaskApiPushService";
    private static final Set<String> SUPPORTED_ENCRYPT_TYPES = Set.of("PLAIN", "AES", "AES_ECB", "AES_CBC", "ECB", "CBC");
    private static final Set<String> SUPPORTED_CIPHER_MODES = Set.of("CBC", "ECB");
    private static final Set<String> SUPPORTED_PADDING_MODES = Set.of("PKCS5Padding", "PKCS7Padding", "NoPadding");

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

        List<Long> instIds = page.getRecords().stream()
                .map(Institution::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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
            vo.setProductName(resolveListProductName(inst, products));
            vo.setMerchantType(defaultText(inst.getMerchantType(), "机构"));
            vo.setOpenCities(resolveListOpenCities(inst, products));
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
    public PageResult<InstitutionApiConfigListVO> pageInstitutionApiConfigs(InstitutionQueryDTO query) {
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10L : Math.min(query.getSize(), 100L);

        LambdaQueryWrapper<Institution> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getMerchantType())) {
            wrapper.eq(Institution::getMerchantType, query.getMerchantType().trim());
        }
        if (StringUtils.hasText(query.getMerchantAlias())) {
            String keyword = query.getMerchantAlias().trim();
            wrapper.and(q -> q.like(Institution::getMerchantAlias, keyword)
                    .or()
                    .like(Institution::getInstName, keyword)
                    .or()
                    .like(Institution::getInstCode, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(Institution::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Institution::getUpdatedAt)
                .orderByDesc(Institution::getCreatedAt);

        Page<Institution> page = institutionMapper.selectPage(new Page<>(current, size), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResult.empty(current, size);
        }

        List<InstitutionApiConfigListVO> records = page.getRecords().stream()
                .map(this::toApiConfigListVO)
                .collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInstitution(InstitutionSaveDTO request) {
        String instCode = generateInstCode(request.getMerchantAlias());
        Institution institution = new Institution();
        institution.setInstCode(instCode);
        fillInstitutionBase(institution, request);
        fillInstitutionApiConfig(institution, null);
        institution.setStatus(request.getBusinessStatus() == null ? 1 : request.getBusinessStatus());
        institution.setApiMerchant(defaultZeroOne(request.getApiMerchant(), 1));
        institution.setSpecifiedChannel(request.getSpecifiedChannel());
        institution.setExcludedChannels(request.getExcludedChannels());
        institution.setAccountBalance(BigDecimal.ZERO);
        institution.setRechargeTotal(BigDecimal.ZERO);
        institutionMapper.insert(institution);
        saveOrUpdatePrimaryInstitutionProduct(institution, request);
        return institution.getId();
    }

    @Override
    public InstitutionDetailVO getInstitutionDetail(Long instId) {
        Institution institution = institutionMapper.selectById(instId);
        if (institution == null) {
            throw new BizException("institution not found: " + instId);
        }
        return toDetailVO(institution);
    }

    @Override
    public InstitutionApiConfigDetailVO getInstitutionApiConfigDetail(Long instId) {
        Institution institution = institutionMapper.selectById(instId);
        if (institution == null) {
            throw new BizException("institution not found: " + instId);
        }
        return toApiConfigDetailVO(institution);
    }

    @Override
    public InstitutionApiConfigOptionsVO getInstitutionApiConfigOptions() {
        InstitutionApiConfigOptionsVO vo = new InstitutionApiConfigOptionsVO();
        vo.setBeanOptions(List.of(
                new OptionVO("通易花适配器", "qlqMaskApiPushService"),
                new OptionVO("德立借适配器", "deljApiPushService")
        ));
        vo.setEncryptTypeOptions(buildEncryptTypeOptions());
        vo.setCipherModeOptions(buildCipherModeOptions());
        vo.setPaddingModeOptions(buildPaddingModeOptions());
        vo.setNotifyEncryptTypeOptions(buildEncryptTypeOptions());
        vo.setNotifyCipherModeOptions(buildCipherModeOptions());
        vo.setNotifyPaddingModeOptions(buildPaddingModeOptions());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInstitution(Long instId, InstitutionSaveDTO request) {
        Institution institution = institutionMapper.selectById(instId);
        if (institution == null) {
            throw new BizException("institution not found: " + instId);
        }
        fillInstitutionBase(institution, request);
        institutionMapper.updateById(institution);
        saveOrUpdatePrimaryInstitutionProduct(institution, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInstitutionApiConfig(Long instId, InstitutionApiConfigUpdateDTO request) {
        Institution institution = institutionMapper.selectById(instId);
        if (institution == null) {
            throw new BizException("institution not found: " + instId);
        }
        fillInstitutionApiConfig(institution, request);
        institutionMapper.updateById(institution);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInstitutionStatus(Long instId, InstitutionStatusUpdateDTO request) {
        Institution institution = institutionMapper.selectById(instId);
        if (institution == null) {
            throw new BizException("institution not found: " + instId);
        }
        Integer status = request.getStatus();
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BizException("status must be 0 or 1");
        }
        institutionMapper.update(null, new LambdaUpdateWrapper<Institution>()
                .eq(Institution::getId, instId)
                .set(Institution::getStatus, status)
                .set(Institution::getUpdateBy, AuditOperatorUtil.currentOperator()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInstitution(Long instId) {
        Institution institution = institutionMapper.selectById(instId);
        if (institution == null) {
            throw new BizException("institution not found: " + instId);
        }

        long productCount = institutionProductMapper.selectCount(new LambdaQueryWrapper<InstitutionProduct>()
                .eq(InstitutionProduct::getInstId, instId));
        if (productCount > 0) {
            throw new BizException("delete institution products first");
        }

        long rechargeCount = rechargeRecordMapper.selectCount(new LambdaQueryWrapper<InstitutionRechargeRecord>()
                .eq(InstitutionRechargeRecord::getInstId, instId));
        if (rechargeCount > 0) {
            throw new BizException("institution has recharge records");
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
            throw new BizException("institution not found: " + instId);
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
            throw new BizException("institution not found: " + instId);
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("amount must be greater than 0");
        }

        InstitutionRechargeRecord record = new InstitutionRechargeRecord();
        record.setInstId(instId);
        record.setInstCode(institution.getInstCode());
        record.setMerchantAlias(defaultText(institution.getMerchantAlias(), institution.getInstName()));
        record.setOperatorName(defaultText(request.getOperatorName(), "system"));
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

    private InstitutionDetailVO toDetailVO(Institution institution) {
        InstitutionDetailVO vo = new InstitutionDetailVO();
        InstitutionProduct primaryProduct = findPrimaryProduct(institution.getId());
        vo.setId(institution.getId());
        vo.setInstCode(institution.getInstCode());
        vo.setInstName(institution.getInstName());
        vo.setMerchantAlias(institution.getMerchantAlias());
        vo.setMerchantType(institution.getMerchantType());
        vo.setStatus(institution.getStatus());
        vo.setStatusDesc(resolveStatus(institution.getStatus()));
        vo.setAdminPhone(institution.getAdminPhone());
        vo.setAdminName(institution.getAdminName());
        vo.setAdminRole(institution.getAdminRole());
        vo.setSmsNotify(institution.getSmsNotify());
        vo.setUserStatus(institution.getUserStatus());
        vo.setCrmAutoAssign(institution.getCrmAutoAssign());
        vo.setApiMerchant(institution.getApiMerchant());
        vo.setSpecifiedChannel(institution.getSpecifiedChannel());
        vo.setExcludedChannels(institution.getExcludedChannels());
        vo.setOpenCities(resolveInstitutionOpenCities(institution, primaryProduct));
        vo.setProductName(primaryProduct == null ? null : primaryProduct.getProductName());
        vo.setProductIcon(primaryProduct == null ? null : primaryProduct.getProductIcon());
        vo.setProductAmount(primaryProduct == null ? null : primaryProduct.getMaxAmount());
        vo.setProductRate(primaryProduct == null ? null : primaryProduct.getRate());
        vo.setProductPeriod(primaryProduct == null ? null : primaryProduct.getPeriod());
        vo.setProductProtocol(primaryProduct == null ? null : primaryProduct.getProtocolUrl());
        vo.setAccountBalance(institution.getAccountBalance());
        vo.setRechargeTotal(institution.getRechargeTotal());
        vo.setRemark(institution.getRemark());
        vo.setCreatedAt(institution.getCreatedAt());
        vo.setCreateBy(institution.getCreateBy());
        vo.setUpdatedAt(institution.getUpdatedAt());
        vo.setUpdateBy(institution.getUpdateBy());
        return vo;
    }

    private InstitutionApiConfigListVO toApiConfigListVO(Institution institution) {
        InstitutionApiConfigListVO vo = new InstitutionApiConfigListVO();
        vo.setId(institution.getId());
        vo.setInstCode(institution.getInstCode());
        vo.setInstName(institution.getInstName());
        vo.setMerchantAlias(defaultText(institution.getMerchantAlias(), institution.getInstName()));
        vo.setMerchantType(institution.getMerchantType());
        vo.setBeanName(institution.getApiMethodName());
        vo.setBusinessCode(institution.getBusinessCode());
        vo.setPreCheckUrl(institution.getPreCheckUrl());
        vo.setApiPushUrl(institution.getApiPushUrl());
        vo.setApiNotifyUrl(institution.getApiNotifyUrl());
        vo.setAppKey(institution.getAppKey());
        vo.setEncryptType(institution.getEncryptType());
        vo.setCipherMode(institution.getCipherMode());
        vo.setPaddingMode(institution.getPaddingMode());
        vo.setIvValue(institution.getIvValue());
        vo.setNotifyEncryptType(institution.getNotifyEncryptType());
        vo.setNotifyCipherMode(institution.getNotifyCipherMode());
        vo.setNotifyPaddingMode(institution.getNotifyPaddingMode());
        vo.setNotifyIvValue(institution.getNotifyIvValue());
        vo.setTimeoutMs(institution.getTimeoutMs());
        vo.setStatus(institution.getStatus());
        vo.setStatusDesc(resolveStatus(institution.getStatus()));
        vo.setCreatedAt(institution.getCreatedAt());
        vo.setCreateBy(institution.getCreateBy());
        vo.setUpdatedAt(institution.getUpdatedAt());
        vo.setUpdateBy(institution.getUpdateBy());
        return vo;
    }

    private InstitutionApiConfigDetailVO toApiConfigDetailVO(Institution institution) {
        InstitutionApiConfigDetailVO vo = new InstitutionApiConfigDetailVO();
        vo.setId(institution.getId());
        vo.setInstCode(institution.getInstCode());
        vo.setInstName(institution.getInstName());
        vo.setMerchantAlias(defaultText(institution.getMerchantAlias(), institution.getInstName()));
        vo.setMerchantType(institution.getMerchantType());
        vo.setBeanName(institution.getApiMethodName());
        vo.setBusinessCode(institution.getBusinessCode());
        vo.setPreCheckUrl(institution.getPreCheckUrl());
        vo.setApiPushUrl(institution.getApiPushUrl());
        vo.setApiNotifyUrl(institution.getApiNotifyUrl());
        vo.setAppKey(institution.getAppKey());
        vo.setEncryptType(institution.getEncryptType());
        vo.setCipherMode(institution.getCipherMode());
        vo.setPaddingMode(institution.getPaddingMode());
        vo.setIvValue(institution.getIvValue());
        vo.setNotifyEncryptType(institution.getNotifyEncryptType());
        vo.setNotifyCipherMode(institution.getNotifyCipherMode());
        vo.setNotifyPaddingMode(institution.getNotifyPaddingMode());
        vo.setNotifyIvValue(institution.getNotifyIvValue());
        vo.setTimeoutMs(institution.getTimeoutMs());
        vo.setStatus(institution.getStatus());
        vo.setStatusDesc(resolveStatus(institution.getStatus()));
        vo.setCreatedAt(institution.getCreatedAt());
        vo.setCreateBy(institution.getCreateBy());
        vo.setUpdatedAt(institution.getUpdatedAt());
        vo.setUpdateBy(institution.getUpdateBy());
        return vo;
    }

    private void fillInstitutionBase(Institution institution, InstitutionSaveDTO request) {
        institution.setInstName(request.getInstName().trim());
        institution.setMerchantAlias(request.getMerchantAlias().trim());
        institution.setMerchantType(defaultText(request.getMerchantType(), "机构"));
        institution.setStatus(request.getBusinessStatus() == null ? 1 : request.getBusinessStatus());
        institution.setAdminPhone(request.getAdminPhone());
        institution.setAdminName(request.getAdminName());
        institution.setAdminRole(defaultText(request.getAdminRole(), "管理员"));
        institution.setSmsNotify(defaultZeroOne(request.getSmsNotify(), 0));
        institution.setUserStatus(defaultZeroOne(request.getUserStatus(), 1));
        institution.setCrmAutoAssign(defaultZeroOne(request.getCrmAutoAssign(), 0));
        institution.setApiMerchant(defaultZeroOne(request.getApiMerchant(), 1));
        institution.setSpecifiedChannel(request.getSpecifiedChannel());
        institution.setExcludedChannels(request.getExcludedChannels());
        institution.setOpenCities(resolveOpenCities(request));
        institution.setRemark(request.getRemark());
    }

    private void fillInstitutionApiConfig(Institution institution, InstitutionApiConfigUpdateDTO request) {
        String instCode = institution.getInstCode();
        institution.setBusinessCode(defaultText(request == null ? null : request.getBusinessCode(), instCode));
        institution.setPreCheckUrl(defaultText(request == null ? null : request.getPreCheckUrl(), "http://127.0.0.1/mock/" + instCode + "/check"));
        institution.setApiPushUrl(defaultText(request == null ? null : request.getApiPushUrl(), "http://127.0.0.1/mock/" + instCode + "/push"));
        institution.setApiNotifyUrl(defaultText(request == null ? null : request.getApiNotifyUrl(), buildDefaultNotifyUrl(instCode)));
        institution.setAppKey(defaultText(request == null ? null : request.getAppKey(), "1234567890abcdef"));
        institution.setEncryptType(normalizeEncryptType(request == null ? null : request.getEncryptType(), "PLAIN"));
        institution.setCipherMode(normalizeCipherMode(request == null ? null : request.getCipherMode(), institution.getEncryptType()));
        institution.setPaddingMode(normalizePaddingMode(request == null ? null : request.getPaddingMode(), "PKCS5Padding"));
        institution.setIvValue(trimToNull(request == null ? null : request.getIvValue()));
        institution.setNotifyEncryptType(normalizeEncryptType(request == null ? null : request.getNotifyEncryptType(), "PLAIN"));
        institution.setNotifyCipherMode(normalizeCipherMode(request == null ? null : request.getNotifyCipherMode(), institution.getNotifyEncryptType()));
        institution.setNotifyPaddingMode(normalizePaddingMode(request == null ? null : request.getNotifyPaddingMode(), "PKCS5Padding"));
        institution.setNotifyIvValue(trimToNull(request == null ? null : request.getNotifyIvValue()));
        institution.setTimeoutMs(request == null || request.getTimeoutMs() == null ? 3000 : request.getTimeoutMs());
        institution.setApiMethodName(defaultText(request == null ? null : request.getBeanName(), DEFAULT_ADAPTER_KEY));
    }

    private String buildDefaultNotifyUrl(String instCode) {
        return "http://127.0.0.1:8082/loan-app/api/notify/" + instCode;
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
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeEncryptType(String value, String fallback) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
        if (!SUPPORTED_ENCRYPT_TYPES.contains(normalized)) {
            throw new BizException("unsupported encryptType: " + normalized);
        }
        return normalized;
    }

    private List<OptionVO> buildEncryptTypeOptions() {
        return List.of(
                new OptionVO("PLAIN（明文）", "PLAIN"),
                new OptionVO("AES/CBC/PKCS5Padding", "AES"),
                new OptionVO("AES/ECB/PKCS5Padding", "AES_ECB"),
                new OptionVO("AES/CBC/PKCS5Padding（显式）", "AES_CBC")
        );
    }

    private String normalizeCipherMode(String value, String encryptType) {
        String fallback = defaultCipherMode(encryptType);
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
        if (!SUPPORTED_CIPHER_MODES.contains(normalized)) {
            throw new BizException("unsupported cipherMode: " + normalized);
        }
        return normalized;
    }

    private String normalizePaddingMode(String value, String fallback) {
        String normalized = StringUtils.hasText(value) ? value.trim() : fallback;
        if (!SUPPORTED_PADDING_MODES.contains(normalized)) {
            throw new BizException("unsupported paddingMode: " + normalized);
        }
        return normalized;
    }

    private String defaultCipherMode(String encryptType) {
        String normalized = normalizeEncryptType(encryptType, "PLAIN");
        return switch (normalized) {
            case "AES_ECB", "ECB" -> "ECB";
            case "PLAIN" -> "ECB";
            default -> "CBC";
        };
    }

    private List<OptionVO> buildCipherModeOptions() {
        return List.of(
                new OptionVO("CBC", "CBC"),
                new OptionVO("ECB", "ECB")
        );
    }

    private List<OptionVO> buildPaddingModeOptions() {
        return List.of(
                new OptionVO("PKCS5Padding", "PKCS5Padding"),
                new OptionVO("PKCS7Padding", "PKCS7Padding"),
                new OptionVO("NoPadding", "NoPadding")
        );
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

    private String resolveListProductName(Institution institution, List<InstitutionProduct> products) {
        String productNames = joinProductNames(products);
        if (StringUtils.hasText(productNames) && !"-".equals(productNames)) {
            return productNames;
        }
        return defaultText(institution.getInstName(), defaultText(institution.getMerchantAlias(), "-"));
    }

    private String resolveListOpenCities(Institution institution, List<InstitutionProduct> products) {
        String productCities = joinProductCities(products);
        if (StringUtils.hasText(productCities) && !"-".equals(productCities)) {
            return productCities;
        }
        return defaultText(institution.getOpenCities(), "-");
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
                    for (String city : cityNames.split("[,，、\n]")) {
                        String trimmed = city == null ? null : city.trim();
                        if (StringUtils.hasText(trimmed)) {
                            citySet.add(trimmed);
                        }
                    }
                });
        return citySet.isEmpty() ? "-" : String.join(",", citySet);
    }

    private void saveOrUpdatePrimaryInstitutionProduct(Institution institution, InstitutionSaveDTO request) {
        InstitutionProduct product = findPrimaryProduct(institution.getId());
        if (product == null) {
            product = new InstitutionProduct();
            product.setInstId(institution.getId());
        }
        product.setProductName(defaultText(request.getProductName(), defaultText(institution.getInstName(), institution.getMerchantAlias())));
        product.setProductIcon(request.getProductIcon());
        product.setMaxAmount(request.getProductAmount());
        product.setRate(request.getProductRate());
        product.setPeriod(request.getProductPeriod());
        product.setProtocolUrl(request.getProductProtocol());
        product.setCityNames(resolveOpenCities(request));
        product.setStatus(institution.getStatus());
        product.setSpecifiedChannels(institution.getSpecifiedChannel());
        product.setExcludedChannels(institution.getExcludedChannels());
        product.setRemark(defaultText(request.getRemark(), "default product created with institution"));
        if (product.getId() == null) {
            institutionProductMapper.insert(product);
            return;
        }
        institutionProductMapper.updateById(product);
    }

    private InstitutionProduct findPrimaryProduct(Long instId) {
        List<InstitutionProduct> products = institutionProductMapper.selectList(new LambdaQueryWrapper<InstitutionProduct>()
                .eq(InstitutionProduct::getInstId, instId)
                .orderByAsc(InstitutionProduct::getCreatedAt)
                .orderByAsc(InstitutionProduct::getId));
        return products.isEmpty() ? null : products.get(0);
    }

    private String resolveOpenCities(InstitutionSaveDTO request) {
        if (StringUtils.hasText(request.getOpenCities())) {
            return request.getOpenCities().trim();
        }
        if (request.getCityCodes() == null || request.getCityCodes().isEmpty()) {
            return null;
        }
        List<String> cities = new ArrayList<>();
        for (String cityCode : request.getCityCodes()) {
            if (StringUtils.hasText(cityCode)) {
                cities.add(cityCode.trim());
            }
        }
        return cities.isEmpty() ? null : String.join(",", cities);
    }

    private String resolveInstitutionOpenCities(Institution institution, InstitutionProduct primaryProduct) {
        if (StringUtils.hasText(institution.getOpenCities())) {
            return institution.getOpenCities();
        }
        return primaryProduct == null ? null : primaryProduct.getCityNames();
    }
}
