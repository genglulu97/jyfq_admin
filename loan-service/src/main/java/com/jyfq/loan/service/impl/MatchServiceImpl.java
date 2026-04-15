package com.jyfq.loan.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jyfq.loan.common.util.TimeUtil;
import com.jyfq.loan.mapper.InstitutionProductMapper;
import com.jyfq.loan.model.common.QualificationConditionGroup;
import com.jyfq.loan.model.common.QualificationRules;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.model.entity.InstitutionProduct;
import com.jyfq.loan.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Product matching service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private static final String TOKEN_HAS = "HAS";
    private static final String TOKEN_NONE = "NONE";
    private static final String TOKEN_NO_OVERDUE = "NO_OVERDUE";
    private static final String TOKEN_HAS_OVERDUE = "HAS_OVERDUE";

    private final InstitutionProductMapper productMapper;

    @Override
    public List<InstitutionProduct> findMatchedProducts(StandardApplyData data) {
        log.info("[MATCH] start, phoneMd5={}, channelCode={}, cityCode={}, age={}, amount={}",
                data.getPhoneMd5(), data.getChannelCode(), data.getCityCode(), data.getAge(), data.getLoanAmount());

        List<InstitutionProduct> candidateProducts = productMapper.matchProducts(
                data.getCityCode(),
                data.getAge(),
                data.getLoanAmount() != null ? data.getLoanAmount() : 0
        );

        if (candidateProducts.isEmpty()) {
            log.info("[MATCH] no products, phoneMd5={}, channelCode={}", data.getPhoneMd5(), data.getChannelCode());
            return candidateProducts;
        }

        return candidateProducts.stream()
                .filter(p -> filterBySpecifiedChannels(p, data))
                .filter(p -> filterByCities(p, data))
                .filter(p -> filterByExcludedCities(p, data))
                .filter(p -> filterByExcludedChannels(p, data))
                .filter(p -> filterByWorkingHours(p))
                .filter(p -> filterByQualifications(p, data))
                .collect(Collectors.toList());
    }

    private boolean filterBySpecifiedChannels(InstitutionProduct product, StandardApplyData data) {
        if (StringUtils.isBlank(product.getSpecifiedChannels())) {
            return true;
        }
        List<String> specifiedChannels = parseCsv(product.getSpecifiedChannels());
        boolean matched = specifiedChannels.contains(data.getChannelCode());
        if (!matched) {
            log.warn("[MATCH] filtered by specifiedChannels, productId={}, channelCode={}, specified={}",
                    product.getId(), data.getChannelCode(), product.getSpecifiedChannels());
        }
        return matched;
    }

    private boolean filterByCities(InstitutionProduct product, StandardApplyData data) {
        if (StringUtils.isBlank(product.getCityList())) {
            return true;
        }
        List<String> allowedCities = parseJsonOrCsv(product.getCityList());
        boolean matched = allowedCities.isEmpty() || matchesCity(allowedCities, data.getCityCode(), data.getWorkCity());
        if (!matched) {
            log.warn("[MATCH] filtered by city whitelist, productId={}, cityCode={}, workCity={}, cityList={}",
                    product.getId(), data.getCityCode(), data.getWorkCity(), product.getCityList());
        }
        return matched;
    }

    private boolean filterByExcludedCities(InstitutionProduct product, StandardApplyData data) {
        if (StringUtils.isBlank(product.getExcludedCityCodes())) {
            return true;
        }
        List<String> excludedCities = parseJsonOrCsv(product.getExcludedCityCodes());
        boolean excluded = matchesCity(excludedCities, data.getCityCode(), data.getWorkCity());
        if (excluded) {
            log.warn("[MATCH] filtered by city blacklist, productId={}, cityCode={}, excludedCities={}",
                    product.getId(), data.getCityCode(), product.getExcludedCityCodes());
        }
        return !excluded;
    }

    private boolean filterByExcludedChannels(InstitutionProduct product, StandardApplyData data) {
        if (StringUtils.isBlank(product.getExcludedChannels())) {
            return true;
        }

        boolean excluded = parseCsv(product.getExcludedChannels()).contains(data.getChannelCode());
        if (excluded) {
            log.warn("[MATCH] filtered by excludedChannels, productId={}, channelCode={}, excluded={}",
                    product.getId(), data.getChannelCode(), product.getExcludedChannels());
        }
        return !excluded;
    }

    private boolean filterByWorkingHours(InstitutionProduct product) {
        boolean inWork = TimeUtil.isCurrentInSlots(product.getWorkingHours());
        if (!inWork) {
            log.warn("[MATCH] filtered by workingHours, productId={}, workingHours={}",
                    product.getId(), product.getWorkingHours());
        }
        return inWork;
    }

    private boolean filterByQualifications(InstitutionProduct product, StandardApplyData data) {
        if (StringUtils.isBlank(product.getQualificationConfig())) {
            return true;
        }

        try {
            QualificationRules rules = JSON.parseObject(product.getQualificationConfig(), QualificationRules.class);
            boolean matched = matchesRuleGroup(rules == null ? null : rules.getMust(), data, true)
                    && matchesRuleGroup(rules == null ? null : rules.getAny(), data, false);
            if (!matched) {
                log.warn("[MATCH] filtered by qualificationRules, productId={}, rules={}",
                        product.getId(), product.getQualificationConfig());
            }
            return matched;
        } catch (Exception ex) {
            log.error("[MATCH] qualificationConfig parse failed, productId={}", product.getId(), ex);
            return true;
        }
    }

    private boolean matchesRuleGroup(QualificationConditionGroup group, StandardApplyData data, boolean requireAll) {
        if (isEmptyRuleGroup(group)) {
            return true;
        }

        Map<String, Boolean> fieldMatches = new LinkedHashMap<>();
        putMatch(fieldMatches, "profession", matchProfession(group.getProfession(), data.getProfession()));
        putMatch(fieldMatches, "overdue", matchOverdue(group.getOverdue(), data.getOverdue()));
        putMatch(fieldMatches, "loanAmount", matchLoanAmount(group.getLoanAmount(), data.getLoanAmount()));
        putMatch(fieldMatches, "loanTime", matchLoanTime(group.getLoanTime(), data.getLoanTime()));
        putMatch(fieldMatches, "zhima", matchZhima(group.getZhima(), data.getZhima()));
        putMatch(fieldMatches, "socialSecurity", matchDuration(group.getSocialSecurity(), data.getSocialSecurity()));
        putMatch(fieldMatches, "providentFund", matchDuration(group.getProvidentFund(), data.getProvidentFund()));
        putMatch(fieldMatches, "commercialInsurance", matchDuration(group.getCommercialInsurance(), data.getCommercialInsurance()));
        putMatch(fieldMatches, "vehicle", matchBinaryAsset(group.getVehicle(), data.getVehicle(), "有车产", "无车产"));
        putMatch(fieldMatches, "house", matchBinaryAsset(group.getHouse(), data.getHouse(), "有房产", "无房产"));
        putMatch(fieldMatches, "householdRegister", matchHouseholdRegister(group.getHouseholdRegister(), data.getWorkCity(), data.getCityCode()));

        if (fieldMatches.isEmpty()) {
            return true;
        }
        return requireAll
                ? fieldMatches.values().stream().allMatch(Boolean::booleanValue)
                : fieldMatches.values().stream().anyMatch(Boolean::booleanValue);
    }

    private void putMatch(Map<String, Boolean> fieldMatches, String key, Boolean matched) {
        if (matched != null) {
            fieldMatches.put(key, matched);
        }
    }

    private boolean isEmptyRuleGroup(QualificationConditionGroup group) {
        return group == null
                || isEmpty(group.getProfession())
                && isEmpty(group.getOverdue())
                && isEmpty(group.getLoanAmount())
                && isEmpty(group.getLoanTime())
                && isEmpty(group.getZhima())
                && isEmpty(group.getSocialSecurity())
                && isEmpty(group.getProvidentFund())
                && isEmpty(group.getCommercialInsurance())
                && isEmpty(group.getVehicle())
                && isEmpty(group.getHouse())
                && isEmpty(group.getHouseholdRegister());
    }

    private boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
    }

    private Boolean matchProfession(List<String> options, Integer actual) {
        if (isEmpty(options)) {
            return null;
        }
        if (actual == null) {
            return false;
        }
        return options.stream().map(this::normalizeToken).anyMatch(option ->
                Objects.equals(option, String.valueOf(actual))
                        || ("上班族".equals(option) && actual == 1)
                        || ("自由职业".equals(option) && actual == 2)
                        || ("私营企业主".equals(option) && actual == 3)
                        || (("公务员".equals(option) || "公务员/事业单位".equals(option)) && actual == 4));
    }

    private Boolean matchOverdue(List<String> options, Integer actual) {
        if (isEmpty(options)) {
            return null;
        }
        if (actual == null) {
            return false;
        }
        return options.stream().map(this::normalizeToken).anyMatch(option ->
                Objects.equals(option, String.valueOf(actual))
                        || (TOKEN_NO_OVERDUE.equals(option) && actual == 1)
                        || (TOKEN_HAS_OVERDUE.equals(option) && actual == 2)
                        || (("信用良好".equals(option) || "无逾期".equals(option)) && actual == 1)
                        || (("当前逾期中".equals(option) || "有逾期".equals(option)) && actual == 2));
    }

    private Boolean matchLoanAmount(List<String> options, Integer actual) {
        if (isEmpty(options)) {
            return null;
        }
        if (actual == null || actual <= 0) {
            return false;
        }
        return options.stream().map(this::extractNumber).filter(Objects::nonNull).anyMatch(boundary -> {
            if (boundary <= 30000) {
                return actual <= 30000;
            }
            if (boundary <= 50000) {
                return actual > 30000 && actual <= 50000;
            }
            if (boundary <= 100000) {
                return actual > 50000 && actual <= 100000;
            }
            return actual > 100000 && actual <= boundary;
        });
    }

    private Boolean matchLoanTime(List<String> options, Integer actual) {
        if (isEmpty(options)) {
            return null;
        }
        if (actual == null || actual <= 0) {
            return false;
        }
        return options.stream().map(this::extractNumber).filter(Objects::nonNull).anyMatch(month -> actual.equals(month));
    }

    private Boolean matchZhima(List<String> options, Integer actual) {
        if (isEmpty(options)) {
            return null;
        }
        if (actual == null) {
            return false;
        }
        return options.stream().map(this::normalizeToken).anyMatch(option -> {
            if ("无".equals(option) || "不限".equals(option)) {
                return true;
            }
            if ("600以下".equals(option)) {
                return actual < 600;
            }
            if ("600-650".equals(option) || "600~650".equals(option)) {
                return actual >= 600 && actual < 650;
            }
            if ("650-700".equals(option) || "650~700".equals(option)) {
                return actual >= 650 && actual < 700;
            }
            if ("700以上".equals(option) || "700+".equals(option)) {
                return actual >= 700;
            }
            if ("650+".equals(option)) {
                return actual >= 650;
            }
            if ("600+".equals(option)) {
                return actual >= 600;
            }
            Integer explicit = extractNumber(option);
            return explicit != null && actual >= explicit;
        });
    }

    private Boolean matchDuration(List<String> options, Integer actual) {
        if (isEmpty(options)) {
            return null;
        }
        if (actual == null) {
            return false;
        }
        return options.stream().map(this::normalizeToken).anyMatch(option -> {
            if (TOKEN_HAS.equals(option) || "YES".equals(option) || "TRUE".equals(option) || "有".equals(option)) {
                return actual > 0;
            }
            if (TOKEN_NONE.equals(option) || "NO".equals(option) || "FALSE".equals(option)) {
                return actual == 0;
            }
            if ("无".equals(option) || "0".equals(option)) {
                return actual == 0;
            }
            if ("6个月以下".equals(option) || "1".equals(option)) {
                return actual == 1 || actual > 0 && actual < 6;
            }
            if ("6-12个月".equals(option) || "2".equals(option)) {
                return actual == 2 || actual >= 6 && actual < 12;
            }
            if ("12个月以上".equals(option) || "3".equals(option)) {
                return actual == 3 || actual >= 12;
            }
            Integer months = extractNumber(option);
            return months != null && actual >= months;
        });
    }

    private Boolean matchBinaryAsset(List<String> options, Integer actual, String positiveLabel, String negativeLabel) {
        if (isEmpty(options)) {
            return null;
        }
        if (actual == null) {
            return false;
        }
        return options.stream().map(this::normalizeToken).anyMatch(option ->
                Objects.equals(option, String.valueOf(actual))
                        || (TOKEN_HAS.equals(option) && actual == 1)
                        || (TOKEN_NONE.equals(option) && actual == 2)
                        || (positiveLabel.equals(option) && actual == 1)
                        || (negativeLabel.equals(option) && actual == 2));
    }

    private Boolean matchHouseholdRegister(List<String> options, String workCity, String cityCode) {
        if (isEmpty(options)) {
            return null;
        }
        return options.stream().map(this::normalizeToken).anyMatch(option ->
                StringUtils.equalsIgnoreCase(option, normalizeToken(workCity))
                        || StringUtils.equalsIgnoreCase(option, normalizeToken(cityCode))
                        || StringUtils.containsIgnoreCase(workCity, option));
    }

    private String normalizeToken(String value) {
        return value == null ? null : value.replace(" ", "").trim();
    }

    private Integer extractNumber(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        return StringUtils.isBlank(digits) ? null : Integer.parseInt(digits);
    }

    private List<String> parseJsonOrCsv(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        try {
            JSONArray array = JSON.parseArray(value);
            return array == null ? Collections.emptyList() : array.toJavaList(String.class).stream()
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            return parseCsv(value);
        }
    }

    private List<String> parseCsv(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    private boolean matchesCity(List<String> cities, String cityCode, String workCity) {
        if (cities == null || cities.isEmpty()) {
            return false;
        }
        return cities.stream().anyMatch(city ->
                StringUtils.equalsIgnoreCase(city, cityCode)
                        || StringUtils.equalsIgnoreCase(city, workCity)
                        || (StringUtils.isNotBlank(workCity) && workCity.contains(city)));
    }
}
