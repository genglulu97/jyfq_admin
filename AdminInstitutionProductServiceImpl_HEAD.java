package com.jyfq.loan.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.mapper.CityConfigMapper;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.mapper.InstitutionProductMapper;
import com.jyfq.loan.mapper.PushRecordMapper;
import com.jyfq.loan.model.common.QualificationConditionGroup;
import com.jyfq.loan.model.common.QualificationRules;
import com.jyfq.loan.model.dto.InstitutionProductQueryDTO;
import com.jyfq.loan.model.dto.InstitutionProductSaveDTO;
import com.jyfq.loan.model.dto.WorkingHourDTO;
import com.jyfq.loan.model.entity.ApplyOrder;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.entity.CityConfig;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.entity.InstitutionProduct;
import com.jyfq.loan.model.entity.PushRecord;
import com.jyfq.loan.model.vo.InstitutionProductDetailVO;
import com.jyfq.loan.model.vo.InstitutionProductListVO;
import com.jyfq.loan.model.vo.InstitutionProductOptionsVO;
import com.jyfq.loan.model.vo.OptionVO;
import com.jyfq.loan.service.AdminInstitutionProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin institution product management service implementation.
 */
@Service
@RequiredArgsConstructor
public class AdminInstitutionProductServiceImpl implements AdminInstitutionProductService {

    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;
    private static final String TOKEN_HAS = "HAS";
    private static final String TOKEN_NONE = "NONE";
    private static final String TOKEN_NO_OVERDUE = "NO_OVERDUE";
    private static final String TOKEN_HAS_OVERDUE = "HAS_OVERDUE";

    private final InstitutionProductMapper institutionProductMapper;
    private final InstitutionMapper institutionMapper;
    private final CityConfigMapper cityConfigMapper;
    private final ChannelMapper channelMapper;
    private final ApplyOrderMapper applyOrderMapper;
    private final PushRecordMapper pushRecordMapper;

    @Override
    public PageResult<InstitutionProductListVO> pageProducts(InstitutionProductQueryDTO query) {
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10L : Math.min(query.getSize(), 100L);

        LambdaQueryWrapper<InstitutionProduct> wrapper = new LambdaQueryWrapper<>();
        if (query.getInstId() != null) {
            wrapper.eq(InstitutionProduct::getInstId, query.getInstId());
        } else if (StringUtils.hasText(query.getMerchantAlias())) {
            List<Long> instIds = institutionMapper.selectList(new LambdaQueryWrapper<Institution>()
                            .and(w -> w.like(Institution::getMerchantAlias, query.getMerchantAlias().trim())
                                    .or()
                                    .like(Institution::getInstName, query.getMerchantAlias().trim())))
                    .stream()
                    .map(Institution::getId)
                    .collect(Collectors.toList());
            if (instIds.isEmpty()) {
                return PageResult.empty(current, size);
            }
            wrapper.in(InstitutionProduct::getInstId, instIds);
        }
        if (query.getStatus() != null) {
            wrapper.eq(InstitutionProduct::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getCityKeyword())) {
            wrapper.and(w -> w.like(InstitutionProduct::getCityNames, query.getCityKeyword().trim())
                    .or()
                    .like(InstitutionProduct::getExcludedCityNames, query.getCityKeyword().trim()));
        }
        wrapper.orderByDesc(InstitutionProduct::getCreatedAt).orderByDesc(InstitutionProduct::getId);

        Page<InstitutionProduct> page = institutionProductMapper.selectPage(new Page<>(current, size), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResult.empty(current, size);
        }

        Map<Long, Institution> institutionMap = buildInstitutionMap(page.getRecords().stream()
                .map(InstitutionProduct::getInstId)
                .collect(Collectors.toList()));

        List<InstitutionProductListVO> records = page.getRecords().stream()
                .map(product -> toListVO(product, institutionMap.get(product.getInstId())))
                .collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public InstitutionProductDetailVO getDetail(Long id) {
        InstitutionProduct product = requireProduct(id);
        Institution institution = institutionMapper.selectById(product.getInstId());

        Map<String, String> channelNameMap = buildChannelNameMap(mergeChannelCodes(product));

        InstitutionProductDetailVO vo = new InstitutionProductDetailVO();
        vo.setId(product.getId());
        vo.setInstId(product.getInstId());
        vo.setMerchantName(institution == null ? product.getProductName() : defaultText(institution.getInstName(), product.getProductName()));
        vo.setMerchantAlias(institution == null ? product.getProductName() : defaultText(institution.getMerchantAlias(), institution.getInstName()));
        vo.setStatus(product.getStatus());
        vo.setStatusDesc(resolveStatusDesc(product.getStatus()));
        vo.setMinAge(product.getMinAge());
        vo.setMaxAge(product.getMaxAge());
        vo.setCityCodes(parseJsonArray(product.getCityList()));
        vo.setCityNames(parseTextList(product.getCityNames()));
        vo.setExcludedCityCodes(parseJsonArray(product.getExcludedCityCodes()));
        vo.setExcludedCityNames(parseTextList(product.getExcludedCityNames()));
        vo.setUnitPrice(defaultDecimal(product.getUnitPrice()));
        vo.setPriceRatio(product.getPriceRatio());
        vo.setCityQuota(defaultInt(product.getDailyQuota(), 0));
        vo.setWeight(defaultInt(product.getWeight(), 100));
        List<String> specifiedChannelCodes = parseCsv(product.getSpecifiedChannels());
        List<String> excludedChannelCodes = parseCsv(product.getExcludedChannels());
        vo.setSpecifiedChannelCodes(specifiedChannelCodes);
        vo.setSpecifiedChannelNames(resolveChannelNames(specifiedChannelCodes, channelNameMap));
        vo.setExcludedChannelCodes(excludedChannelCodes);
        vo.setExcludedChannelNames(resolveChannelNames(excludedChannelCodes, channelNameMap));
        vo.setWorkingHours(parseWorkingHours(product.getWorkingHours()));
        vo.setMinAmount(product.getMinAmount());
        vo.setMaxAmount(product.getMaxAmount());
        vo.setLoanAmountRangeDesc(formatAmountRange(product.getMinAmount(), product.getMaxAmount()));
        vo.setQualificationRules(parseQualificationRules(product.getQualificationConfig()));

        QualificationConditionGroup preferredGroup = firstNonEmptyRuleGroup(vo.getQualificationRules());
        vo.setProvidentFund(resolveBinaryValue(preferredGroup == null ? null : preferredGroup.getProvidentFund()));
        vo.setProvidentFundDesc(resolveBinaryRequirement(vo.getProvidentFund()));
        vo.setSocialSecurity(resolveBinaryValue(preferredGroup == null ? null : preferredGroup.getSocialSecurity()));
        vo.setSocialSecurityDesc(resolveBinaryRequirement(vo.getSocialSecurity()));
        vo.setZhimaLevel(resolveZhimaLevel(preferredGroup == null ? null : preferredGroup.getZhima()));
        vo.setCommercialInsurance(resolveBinaryValue(preferredGroup == null ? null : preferredGroup.getCommercialInsurance()));
        vo.setCommercialInsuranceDesc(resolveBinaryRequirement(vo.getCommercialInsurance()));
        vo.setProfession(resolveProfessionValue(preferredGroup == null ? null : preferredGroup.getProfession()));
        vo.setProfessionDesc(resolveProfession(vo.getProfession()));
        vo.setHouse(resolveBinaryValue(preferredGroup == null ? null : preferredGroup.getHouse()));
        vo.setHouseDesc(resolveBinaryRequirement(vo.getHouse()));
        vo.setVehicle(resolveBinaryValue(preferredGroup == null ? null : preferredGroup.getVehicle()));
        vo.setVehicleDesc(resolveBinaryRequirement(vo.getVehicle()));
        vo.setOverdue(resolveOverdueValue(preferredGroup == null ? null : preferredGroup.getOverdue()));
        vo.setOverdueDesc(resolveOverdueRequirement(vo.getOverdue()));
        vo.setHouseholdRegister(resolveFirstOption(preferredGroup == null ? null : preferredGroup.getHouseholdRegister()));
        vo.setRemark(product.getRemark());
        vo.setCreatedAt(product.getCreatedAt());
        vo.setCreateBy(product.getCreateBy());
        vo.setUpdatedAt(product.getUpdatedAt());
        vo.setUpdateBy(product.getUpdateBy());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(InstitutionProductSaveDTO request) {
        Institution institution = requireInstitution(request.getInstId());
        validateSaveRequest(request, institution, null);

        InstitutionProduct product = new InstitutionProduct();
        fillProduct(product, request, institution, findTemplateProduct(request.getInstId(), null));
        institutionProductMapper.insert(product);
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long id, InstitutionProductSaveDTO request) {
        InstitutionProduct existing = requireProduct(id);
        Institution institution = requireInstitution(request.getInstId());
        validateSaveRequest(request, institution, id);

        InstitutionProduct updated = new InstitutionProduct();
        updated.setId(id);
        fillProduct(updated, request, institution, existing);
        institutionProductMapper.updateById(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        requireProduct(id);
        long applyRefCount = applyOrderMapper.selectCount(new LambdaQueryWrapper<ApplyOrder>()
                .eq(ApplyOrder::getProductId, id));
        long pushRefCount = pushRecordMapper.selectCount(new LambdaQueryWrapper<PushRecord>()
                .eq(PushRecord::getProductId, id));
        if (applyRefCount > 0 || pushRefCount > 0) {
            throw new BizException("当前产品已有历史订单或推单记录，不能删除");
        }
        institutionProductMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyProduct(Long id) {
        InstitutionProduct source = requireProduct(id);
        InstitutionProduct copy = new InstitutionProduct();
        copy.setInstId(source.getInstId());
        copy.setProductName(source.getProductName());
        copy.setProductIcon(source.getProductIcon());
        copy.setMinAge(source.getMinAge());
        copy.setMaxAge(source.getMaxAge());
        copy.setMinAmount(source.getMinAmount());
        copy.setMaxAmount(source.getMaxAmount());
        copy.setRate(source.getRate());
        copy.setPeriod(source.getPeriod());
        copy.setProtocolUrl(source.getProtocolUrl());
        copy.setCityNames(source.getCityNames());
        copy.setExcludedCityCodes(source.getExcludedCityCodes());
        copy.setExcludedCityNames(source.getExcludedCityNames());
        copy.setCityMode(source.getCityMode());
        copy.setCityList(source.getCityList());
        copy.setWorkingHours(source.getWorkingHours());
        copy.setSpecifiedChannels(source.getSpecifiedChannels());
        copy.setExcludedChannels(source.getExcludedChannels());
        copy.setQualificationConfig(source.getQualificationConfig());
        copy.setPriority(source.getPriority());
        copy.setWeight(source.getWeight());
        copy.setDailyQuota(source.getDailyQuota());
        copy.setUnitPrice(source.getUnitPrice());
        copy.setPriceRatio(source.getPriceRatio());
        copy.setRemark(source.getRemark());
        copy.setExtJson(source.getExtJson());
        copy.setStatus(source.getStatus());
        institutionProductMapper.insert(copy);
        return copy.getId();
    }

    @Override
    public InstitutionProductOptionsVO getOptions() {
        InstitutionProductOptionsVO vo = new InstitutionProductOptionsVO();

        List<Institution> institutions = institutionMapper.selectList(new LambdaQueryWrapper<Institution>()
                .orderByDesc(Institution::getCreatedAt)
                .orderByDesc(Institution::getId));
        vo.setMerchants(institutions.stream()
                .map(inst -> new OptionVO(defaultText(inst.getMerchantAlias(), inst.getInstName()), String.valueOf(inst.getId())))
                .collect(Collectors.toList()));

        List<Channel> channels = channelMapper.selectList(new LambdaQueryWrapper<Channel>()
                .orderByDesc(Channel::getCreatedAt)
                .orderByDesc(Channel::getId));
        vo.setChannels(channels.stream()
                .map(channel -> new OptionVO(channel.getChannelName(), channel.getChannelCode()))
                .collect(Collectors.toList()));

        vo.setCities(cityConfigMapper.selectList(new LambdaQueryWrapper<CityConfig>()
                        .eq(CityConfig::getStatus, STATUS_ENABLED)
                        .orderByAsc(CityConfig::getSort)
                        .orderByAsc(CityConfig::getCityCode))
                .stream()
                .map(city -> new OptionVO(city.getCityName(), city.getCityCode()))
                .collect(Collectors.toList()));

        vo.setStatusOptions(Arrays.asList(
                new OptionVO("启用", String.valueOf(STATUS_ENABLED)),
                new OptionVO("禁用", String.valueOf(STATUS_DISABLED))
        ));
        vo.setBinaryOptions(Arrays.asList(
                new OptionVO("不限", "0"),
                new OptionVO("有", "1"),
                new OptionVO("无", "2")
        ));
        vo.setProfessionOptions(Arrays.asList(
                new OptionVO("不限", "0"),
                new OptionVO("上班族", "1"),
                new OptionVO("自由职业", "2"),
                new OptionVO("企业主", "3"),
                new OptionVO("公职人员", "4")
        ));
        vo.setOverdueOptions(Arrays.asList(
                new OptionVO("不限", "0"),
                new OptionVO("无逾期", "1"),
                new OptionVO("有逾期", "2")
        ));
        vo.setZhimaOptions(Arrays.asList(
                new OptionVO("不限", ""),
                new OptionVO("600+", "600+"),
                new OptionVO("650+", "650+"),
                new OptionVO("700+", "700+")
        ));
        vo.setWeekOptions(Arrays.asList(
                new OptionVO("周一", "MONDAY"),
                new OptionVO("周二", "TUESDAY"),
                new OptionVO("周三", "WEDNESDAY"),
                new OptionVO("周四", "THURSDAY"),
                new OptionVO("周五", "FRIDAY"),
                new OptionVO("周六", "SATURDAY"),
                new OptionVO("周日", "SUNDAY")
        ));
        return vo;
    }

    private InstitutionProduct requireProduct(Long id) {
        InstitutionProduct product = institutionProductMapper.selectById(id);
        if (product == null) {
            throw new BizException("机构产品不存在: " + id);
        }
        return product;
    }

    private Institution requireInstitution(Long instId) {
        Institution institution = institutionMapper.selectById(instId);
        if (institution == null) {
            throw new BizException("机构不存在: " + instId);
        }
        return institution;
    }

    private void validateSaveRequest(InstitutionProductSaveDTO request, Institution institution, Long currentId) {
        if (request.getMinAge() > request.getMaxAge()) {
            throw new BizException("年龄区间不合法");
        }
        if (request.getMinAmount() > request.getMaxAmount()) {
            throw new BizException("贷款额度区间不合法");
        }
        List<String> specified = normalizeDistinct(request.getSpecifiedChannelCodes());
        List<String> excluded = normalizeDistinct(request.getExcludedChannelCodes());
        Set<String> overlapChannels = new LinkedHashSet<>(specified);
        overlapChannels.retainAll(excluded);
        if (!overlapChannels.isEmpty()) {
            throw new BizException("指定渠道和拒绝渠道不能重复");
        }
        validateChannelsExist(specified);
        validateChannelsExist(excluded);
        if (currentId == null) {
            return;
        }
        InstitutionProduct existing = requireProduct(currentId);
        if (!Objects.equals(existing.getInstId(), request.getInstId())) {
            long applyRefCount = applyOrderMapper.selectCount(new LambdaQueryWrapper<ApplyOrder>()
                    .eq(ApplyOrder::getProductId, currentId));
            if (applyRefCount > 0) {
                throw new BizException("历史订单已关联当前产品，不能切换所属机构");
            }
        }
    }

    private void validateChannelsExist(List<String> channelCodes) {
        if (channelCodes.isEmpty()) {
            return;
        }
        long count = channelMapper.selectCount(new LambdaQueryWrapper<Channel>()
                .in(Channel::getChannelCode, channelCodes));
        if (count != channelCodes.size()) {
            throw new BizException("存在无效的渠道配置");
        }
    }

    private void fillProduct(InstitutionProduct target, InstitutionProductSaveDTO request, Institution institution, InstitutionProduct template) {
        target.setInstId(request.getInstId());
        target.setProductName(defaultText(institution.getMerchantAlias(), institution.getInstName()));
        target.setProductIcon(template == null ? null : template.getProductIcon());
        target.setRate(template == null ? null : template.getRate());
        target.setPeriod(template == null ? null : template.getPeriod());
        target.setProtocolUrl(template == null ? null : template.getProtocolUrl());
        target.setMinAge(request.getMinAge());
        target.setMaxAge(request.getMaxAge());
        target.setMinAmount(request.getMinAmount());
        target.setMaxAmount(request.getMaxAmount());
        List<String> cityCodes = normalizeDistinct(firstNonEmpty(request.getCityCodes(), request.getCityNames()));
        List<String> cityNames = normalizeDistinct(firstNonEmpty(request.getCityNames(), request.getCityCodes()));
        List<String> excludedCityCodes = normalizeDistinct(firstNonEmpty(request.getExcludedCityCodes(), request.getExcludedCityNames()));
        List<String> excludedCityNames = normalizeDistinct(firstNonEmpty(request.getExcludedCityNames(), request.getExcludedCityCodes()));
        target.setCityMode(cityCodes.isEmpty() ? 0 : 1);
        target.setCityList(toJsonArray(cityCodes));
        target.setCityNames(joinText(cityNames));
        target.setExcludedCityCodes(toJsonArray(excludedCityCodes));
        target.setExcludedCityNames(joinText(excludedCityNames));
        target.setWorkingHours(toWorkingHoursJson(request.getWorkingHours()));
        target.setSpecifiedChannels(joinCsv(normalizeDistinct(request.getSpecifiedChannelCodes())));
        target.setExcludedChannels(joinCsv(normalizeDistinct(request.getExcludedChannelCodes())));
        target.setQualificationConfig(buildQualificationConfig(request));
        target.setWeight(defaultInt(request.getWeight(), 100));
        target.setPriority(toPriority(target.getWeight()));
        target.setDailyQuota(defaultInt(request.getDailyQuota(), 0));
        target.setUnitPrice(defaultDecimal(request.getUnitPrice()));
        target.setPriceRatio(request.getPriceRatio());
        target.setRemark(trimToNull(request.getRemark()));
        target.setExtJson(template == null ? null : template.getExtJson());
        target.setStatus(defaultInt(request.getStatus(), STATUS_ENABLED));
    }

    private InstitutionProduct findTemplateProduct(Long instId, Long currentId) {
        List<InstitutionProduct> products = institutionProductMapper.selectList(new LambdaQueryWrapper<InstitutionProduct>()
                .eq(InstitutionProduct::getInstId, instId)
                .orderByDesc(InstitutionProduct::getCreatedAt)
                .orderByDesc(InstitutionProduct::getId));
        if (products.isEmpty()) {
            return null;
        }
        if (currentId == null) {
            return products.get(0);
        }
        return products.stream()
                .filter(item -> Objects.equals(item.getId(), currentId))
                .findFirst()
                .orElse(products.get(0));
    }

    private InstitutionProductListVO toListVO(InstitutionProduct product, Institution institution) {
        InstitutionProductListVO vo = new InstitutionProductListVO();
        vo.setId(product.getId());
        vo.setInstId(product.getInstId());
        vo.setMerchantAlias(institution == null ? product.getProductName() : defaultText(institution.getMerchantAlias(), institution.getInstName()));
        vo.setStatus(product.getStatus());
        vo.setStatusDesc(resolveStatusDesc(product.getStatus()));
        vo.setCityNames(defaultText(product.getCityNames(), "-"));
        vo.setUnitPrice(defaultDecimal(product.getUnitPrice()));
        vo.setCityQuota(defaultInt(product.getDailyQuota(), 0));
        vo.setWeight(defaultInt(product.getWeight(), 100));
        vo.setCreatedAt(product.getCreatedAt());
        vo.setCreateBy(product.getCreateBy());
        vo.setUpdatedAt(product.getUpdatedAt());
        vo.setUpdateBy(product.getUpdateBy());
        vo.setRemark(defaultText(product.getRemark(), "-"));
        return vo;
    }

    private QualificationRules parseQualificationRules(String qualificationConfig) {
        if (!StringUtils.hasText(qualificationConfig)) {
            return new QualificationRules();
        }
        try {
            QualificationRules rules = JSON.parseObject(qualificationConfig, QualificationRules.class);
            return rules == null ? new QualificationRules() : rules;
        } catch (Exception ex) {
            return new QualificationRules();
        }
    }

    private String buildQualificationConfig(InstitutionProductSaveDTO request) {
        QualificationRules normalized = normalizeQualificationRules(request.getQualificationRules());
        QualificationConditionGroup mustGroup = normalized.getMust() == null ? new QualificationConditionGroup() : normalized.getMust();
        appendSingleOption(mustGroup::getProfession, mustGroup::setProfession, normalizeProfessionToken(request.getProfession()));
        appendSingleOption(mustGroup::getOverdue, mustGroup::setOverdue, normalizeOverdueToken(request.getOverdue()));
        appendSingleOption(mustGroup::getZhima, mustGroup::setZhima, trimToNull(request.getZhimaLevel()));
        appendSingleOption(mustGroup::getSocialSecurity, mustGroup::setSocialSecurity, normalizeBinaryToken(request.getSocialSecurity()));
        appendSingleOption(mustGroup::getProvidentFund, mustGroup::setProvidentFund, normalizeBinaryToken(request.getProvidentFund()));
        appendSingleOption(mustGroup::getCommercialInsurance, mustGroup::setCommercialInsurance, normalizeBinaryToken(request.getCommercialInsurance()));
        appendSingleOption(mustGroup::getVehicle, mustGroup::setVehicle, normalizeBinaryToken(request.getVehicle()));
        appendSingleOption(mustGroup::getHouse, mustGroup::setHouse, normalizeBinaryToken(request.getHouse()));
        appendSingleOption(mustGroup::getHouseholdRegister, mustGroup::setHouseholdRegister, trimToNull(request.getHouseholdRegister()));
        normalized.setMust(normalizeRuleGroup(mustGroup));
        if (isEmptyRuleGroup(normalized.getMust()) && isEmptyRuleGroup(normalized.getAny())) {
            return null;
        }
        return JSON.toJSONString(normalized);
    }

    private QualificationRules normalizeQualificationRules(QualificationRules rules) {
        QualificationRules normalized = new QualificationRules();
        if (rules == null) {
            return normalized;
        }
        normalized.setMust(normalizeRuleGroup(rules.getMust()));
        normalized.setAny(normalizeRuleGroup(rules.getAny()));
        return normalized;
    }

    private QualificationConditionGroup firstNonEmptyRuleGroup(QualificationRules rules) {
        if (rules == null) {
            return null;
        }
        if (!isEmptyRuleGroup(rules.getMust())) {
            return rules.getMust();
        }
        if (!isEmptyRuleGroup(rules.getAny())) {
            return rules.getAny();
        }
        return null;
    }

    private void appendSingleOption(java.util.function.Supplier<List<String>> getter,
                                    java.util.function.Consumer<List<String>> setter,
                                    String option) {
        if (!StringUtils.hasText(option)) {
            return;
        }
        List<String> merged = getter.get() == null ? new ArrayList<>() : new ArrayList<>(getter.get());
        merged.add(option);
        setter.accept(normalizeDistinct(merged));
    }

    private QualificationConditionGroup normalizeRuleGroup(QualificationConditionGroup group) {
        if (group == null) {
            return null;
        }
        QualificationConditionGroup normalized = new QualificationConditionGroup();
        normalized.setProfession(normalizeDistinct(group.getProfession()));
        normalized.setOverdue(normalizeDistinct(group.getOverdue()));
        normalized.setLoanAmount(normalizeDistinct(group.getLoanAmount()));
        normalized.setLoanTime(normalizeDistinct(group.getLoanTime()));
        normalized.setZhima(normalizeDistinct(group.getZhima()));
        normalized.setSocialSecurity(normalizeDistinct(group.getSocialSecurity()));
        normalized.setProvidentFund(normalizeDistinct(group.getProvidentFund()));
        normalized.setCommercialInsurance(normalizeDistinct(group.getCommercialInsurance()));
        normalized.setVehicle(normalizeDistinct(group.getVehicle()));
        normalized.setHouse(normalizeDistinct(group.getHouse()));
        normalized.setHouseholdRegister(normalizeDistinct(group.getHouseholdRegister()));
        return isEmptyRuleGroup(normalized) ? null : normalized;
    }

    private boolean isEmptyRuleGroup(QualificationConditionGroup group) {
        return group == null
                || CollectionUtils.isEmpty(group.getProfession())
                && CollectionUtils.isEmpty(group.getOverdue())
                && CollectionUtils.isEmpty(group.getLoanAmount())
                && CollectionUtils.isEmpty(group.getLoanTime())
                && CollectionUtils.isEmpty(group.getZhima())
                && CollectionUtils.isEmpty(group.getSocialSecurity())
                && CollectionUtils.isEmpty(group.getProvidentFund())
                && CollectionUtils.isEmpty(group.getCommercialInsurance())
                && CollectionUtils.isEmpty(group.getVehicle())
                && CollectionUtils.isEmpty(group.getHouse())
                && CollectionUtils.isEmpty(group.getHouseholdRegister());
    }

    private Integer resolveZhimaScore(String zhimaLevel) {
        if (!StringUtils.hasText(zhimaLevel)) {
            return null;
        }
        return switch (zhimaLevel.trim()) {
            case "600+" -> 600;
            case "650+" -> 650;
            case "700+" -> 700;
            default -> null;
        };
    }

    private String resolveZhimaLevel(List<String> options) {
        String firstOption = resolveFirstOption(options);
        if (!StringUtils.hasText(firstOption)) {
            return "不限";
        }
        Integer minZhima = resolveZhimaScore(firstOption);
        if (minZhima == null) {
            return firstOption;
        }
        if (minZhima >= 700) {
            return "700+";
        }
        if (minZhima >= 650) {
            return "650+";
        }
        return "600+";
    }

    private String resolveStatusDesc(Integer status) {
        return Integer.valueOf(STATUS_ENABLED).equals(status) ? "启用" : "禁用";
    }

    private String resolveBinaryRequirement(Integer value) {
        if (value == null || value == 0) {
            return "不限";
        }
        return value == 1 ? "有" : "无";
    }

    private String resolveProfession(Integer profession) {
        if (profession == null || profession == 0) {
            return "不限";
        }
        return switch (profession) {
            case 1 -> "上班族";
            case 2 -> "自由职业";
            case 3 -> "企业主";
            case 4 -> "公职人员";
            default -> "不限";
        };
    }

    private String resolveOverdueRequirement(Integer overdue) {
        if (overdue == null || overdue == 0) {
            return "不限";
        }
        return overdue == 1 ? "无逾期" : "有逾期";
    }

    private Integer resolveBinaryValue(List<String> options) {
        String firstOption = resolveFirstOption(options);
        if (!StringUtils.hasText(firstOption)) {
            return 0;
        }
        String normalized = firstOption.trim().toUpperCase();
        if (TOKEN_HAS.equals(normalized) || "1".equals(normalized) || "YES".equals(normalized) || "TRUE".equals(normalized)) {
            return 1;
        }
        if (TOKEN_NONE.equals(normalized) || "2".equals(normalized) || "NO".equals(normalized) || "FALSE".equals(normalized)) {
            return 2;
        }
        return 0;
    }

    private Integer resolveProfessionValue(List<String> options) {
        String firstOption = resolveFirstOption(options);
        if (!StringUtils.hasText(firstOption)) {
            return 0;
        }
        try {
            return Integer.parseInt(firstOption.trim());
        } catch (NumberFormatException ex) {
            return switch (firstOption.trim().toUpperCase()) {
                case "EMPLOYEE" -> 1;
                case "SELF_EMPLOYED" -> 2;
                case "BUSINESS_OWNER" -> 3;
                case "PUBLIC_SERVANT" -> 4;
                default -> 0;
            };
        }
    }

    private Integer resolveOverdueValue(List<String> options) {
        String firstOption = resolveFirstOption(options);
        if (!StringUtils.hasText(firstOption)) {
            return 0;
        }
        String normalized = firstOption.trim().toUpperCase();
        if (TOKEN_NO_OVERDUE.equals(normalized) || "1".equals(normalized)) {
            return 1;
        }
        if (TOKEN_HAS_OVERDUE.equals(normalized) || "2".equals(normalized)) {
            return 2;
        }
        return 0;
    }

    private String resolveFirstOption(List<String> options) {
        if (CollectionUtils.isEmpty(options)) {
            return null;
        }
        return options.stream().filter(StringUtils::hasText).findFirst().orElse(null);
    }

    private String normalizeBinaryToken(Integer value) {
        if (value == null || value == 0) {
            return null;
        }
        return value == 1 ? TOKEN_HAS : TOKEN_NONE;
    }

    private String normalizeProfessionToken(Integer value) {
        if (value == null || value == 0) {
            return null;
        }
        return String.valueOf(value);
    }

    private String normalizeOverdueToken(Integer value) {
        if (value == null || value == 0) {
            return null;
        }
        return value == 1 ? TOKEN_NO_OVERDUE : TOKEN_HAS_OVERDUE;
    }

    private String formatAmountRange(Integer minAmount, Integer maxAmount) {
        if (minAmount == null && maxAmount == null) {
            return "-";
        }
        return defaultInt(minAmount, 0) + "-" + defaultInt(maxAmount, 0);
    }

    private int toPriority(Integer weight) {
        int normalized = weight == null ? 100 : Math.max(1, Math.min(weight, 9999));
        return Math.max(1, 10000 - normalized);
    }

    private String toWorkingHoursJson(List<WorkingHourDTO> workingHours) {
        List<WorkingHourDTO> normalized = workingHours == null ? Collections.emptyList() : workingHours.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getStartTime()) && StringUtils.hasText(item.getEndTime()))
                .collect(Collectors.toList());
        return normalized.isEmpty() ? null : JSON.toJSONString(normalized);
    }

    private List<WorkingHourDTO> parseWorkingHours(String workingHoursJson) {
        if (!StringUtils.hasText(workingHoursJson)) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(workingHoursJson, WorkingHourDTO.class);
        } catch (Exception ex) {
            List<WorkingHourDTO> fallback = new ArrayList<>();
            JSONArray slots = JSON.parseArray(workingHoursJson);
            for (int i = 0; i < slots.size(); i++) {
                JSONObject slot = slots.getJSONObject(i);
                WorkingHourDTO dto = new WorkingHourDTO();
                dto.setDayOfWeek(slot.getString("dayOfWeek"));
                dto.setStartTime(slot.getString("start"));
                dto.setEndTime(slot.getString("end"));
                fallback.add(dto);
            }
            return fallback;
        }
    }

    private Map<Long, Institution> buildInstitutionMap(List<Long> instIds) {
        return buildMap(instIds, institutionMapper::selectBatchIds, Institution::getId);
    }

    private <K, T> Map<K, T> buildMap(List<K> ids, Function<Collection<K>, List<T>> query, Function<T, K> keyMapper) {
        List<K> distinctIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return query.apply(distinctIds).stream()
                .collect(Collectors.toMap(keyMapper, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private Map<String, String> buildChannelNameMap(List<String> channelCodes) {
        if (channelCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return channelMapper.selectList(new LambdaQueryWrapper<Channel>()
                        .in(Channel::getChannelCode, channelCodes))
                .stream()
                .collect(Collectors.toMap(Channel::getChannelCode, Channel::getChannelName, (left, right) -> left, LinkedHashMap::new));
    }

    private List<String> resolveChannelNames(List<String> channelCodes, Map<String, String> channelNameMap) {
        if (channelCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return channelCodes.stream()
                .map(code -> channelNameMap.getOrDefault(code, code))
                .collect(Collectors.toList());
    }

    private List<String> mergeChannelCodes(InstitutionProduct product) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(parseCsv(product.getSpecifiedChannels()));
        merged.addAll(parseCsv(product.getExcludedChannels()));
        return new ArrayList<>(merged);
    }

    private List<String> parseJsonArray(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(json, String.class);
        } catch (Exception ex) {
            return parseTextList(json);
        }
    }

    private List<String> parseCsv(String value) {
        return parseTextList(value);
    }

    private List<String> parseTextList(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split("[,，;；\\n]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> splitCities(String value) {
        return parseTextList(value);
    }

    private List<String> normalizeDistinct(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> firstNonEmpty(List<String> primary, List<String> fallback) {
        return CollectionUtils.isEmpty(primary) ? fallback : primary;
    }

    private String toJsonArray(List<String> values) {
        return values.isEmpty() ? null : JSON.toJSONString(values);
    }

    private String joinCsv(List<String> values) {
        return values.isEmpty() ? null : String.join(",", values);
    }

    private String joinText(List<String> values) {
        return values.isEmpty() ? null : String.join(",", values);
    }

    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
