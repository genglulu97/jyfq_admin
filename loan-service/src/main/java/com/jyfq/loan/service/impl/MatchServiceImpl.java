package com.jyfq.loan.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.jyfq.loan.common.util.TimeUtil;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.mapper.InstitutionProductMapper;
import com.jyfq.loan.model.common.QualificationConditionGroup;
import com.jyfq.loan.model.common.QualificationRules;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.entity.Institution;
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
    private static final String CITY_ALL = "ALL";

    private final InstitutionProductMapper productMapper;
    private final InstitutionMapper institutionMapper;
    private final ChannelMapper channelMapper;

    @Override
    public List<InstitutionProduct> findMatchedProducts(StandardApplyData data) {
        log.info("[MATCH] start, phoneMd5={}, channelCode={}, cityCode={}, age={}, amount={}",
                data.getPhoneMd5(), data.getChannelCode(), data.getCityCode(), data.getAge(), data.getLoanAmount());

        List<InstitutionProduct> candidateProducts = productMapper.matchProducts(
                data.getCityCode(),
                data.getAge(),
                data.getLoanAmount() != null ? data.getLoanAmount() : 0
        );
        log.info("【产品匹配】MD5:【{}】渠道：{}，基础条件匹配数量：{}，产品ID：{}",
                data.getPhoneMd5(), data.getChannelCode(), candidateProducts.size(), buildProductIds(candidateProducts));

        if (candidateProducts.isEmpty()) {
            log.info("[MATCH] no products, phoneMd5={}, channelCode={}", data.getPhoneMd5(), data.getChannelCode());
            return candidateProducts;
        }

        List<InstitutionProduct> matchedProducts = candidateProducts.stream()
                .filter(p -> filterByChannelType(p, data))
                .filter(p -> filterBySpecifiedChannels(p, data))
                .filter(p -> filterByCities(p, data))
                .filter(p -> filterByExcludedCities(p, data))
                .filter(p -> filterByExcludedChannels(p, data))
                .filter(p -> filterByWorkingHours(p))
                .filter(p -> filterByQualifications(p, data))
                .collect(Collectors.toList());
        log.info("【产品匹配】MD5:【{}】渠道：{}，最终匹配数量：{}，匹配产品：{}",
                data.getPhoneMd5(), data.getChannelCode(), matchedProducts.size(), buildProductBriefs(matchedProducts));
        return matchedProducts;
    }

    private boolean filterByChannelType(InstitutionProduct product, StandardApplyData data) {
        Channel channel = channelMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelCode, data.getChannelCode())
                .last("LIMIT 1"));
        if (channel == null || StringUtils.isBlank(channel.getChannelType())) {
            return true;
        }
        Institution institution = institutionMapper.selectById(product.getInstId());
        String institutionType = institution == null ? null : institution.getChannelType();
        boolean matched = StringUtils.isNotBlank(institutionType)
                && channel.getChannelType().trim().equalsIgnoreCase(institutionType.trim());
        if (!matched) {
            log.warn("[MATCH] filtered by channelType, productId={}, channelCode={}, channelType={}, instId={}, instChannelType={}",
                    product.getId(), data.getChannelCode(), channel.getChannelType(), product.getInstId(), institutionType);
        }
        return matched;
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
        boolean matched = allowedCities.isEmpty() || isAllCity(allowedCities) || matchesCity(allowedCities, data.getCityCode(), data.getWorkCity());
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
            RuleGroupMatchResult mustMatch = matchRuleGroup("must", rules == null ? null : rules.getMust(), data, true);
            RuleGroupMatchResult anyMatch = matchRuleGroup("any", rules == null ? null : rules.getAny(), data, false);
            boolean matched = mustMatch.matched() && anyMatch.matched();
            if (!matched) {
                log.warn("[MATCH] filtered by qualificationRules, productId={}, mismatch={}, rules={}",
                        product.getId(), buildQualificationMismatchLog(mustMatch, anyMatch), product.getQualificationConfig());
            }
            return matched;
        } catch (Exception ex) {
            log.error("[MATCH] qualificationConfig parse failed, productId={}", product.getId(), ex);
            return true;
        }
    }

    private RuleGroupMatchResult matchRuleGroup(String groupName, QualificationConditionGroup group,
                                                StandardApplyData data, boolean requireAll) {
        if (isEmptyRuleGroup(group)) {
            return new RuleGroupMatchResult(groupName, requireAll, true, Collections.emptyMap());
        }

        Map<String, RuleFieldMatch> fieldMatches = new LinkedHashMap<>();
        putMatch(fieldMatches, "profession", group.getProfession(), describeProfession(data.getProfession()),
                matchProfession(group.getProfession(), data.getProfession()));
        putMatch(fieldMatches, "overdue", group.getOverdue(), describeOverdue(data.getOverdue()),
                matchOverdue(group.getOverdue(), data.getOverdue()));
        putMatch(fieldMatches, "loanAmount", group.getLoanAmount(), describeAmount(data.getLoanAmount()),
                matchLoanAmount(group.getLoanAmount(), data.getLoanAmount()));
        putMatch(fieldMatches, "loanTime", group.getLoanTime(), describeMonths(data.getLoanTime()),
                matchLoanTime(group.getLoanTime(), data.getLoanTime()));
        putMatch(fieldMatches, "zhima", group.getZhima(), describePlain(data.getZhima()),
                matchZhima(group.getZhima(), data.getZhima()));
        putMatch(fieldMatches, "socialSecurity", group.getSocialSecurity(), describeDuration(data.getSocialSecurity()),
                matchDuration(group.getSocialSecurity(), data.getSocialSecurity()));
        putMatch(fieldMatches, "providentFund", group.getProvidentFund(), describeDuration(data.getProvidentFund()),
                matchDuration(group.getProvidentFund(), data.getProvidentFund()));
        putMatch(fieldMatches, "commercialInsurance", group.getCommercialInsurance(), describeDuration(data.getCommercialInsurance()),
                matchDuration(group.getCommercialInsurance(), data.getCommercialInsurance()));
        putMatch(fieldMatches, "vehicle", group.getVehicle(), describeBinaryAsset(data.getVehicle(), "有车产", "无车产"),
                matchBinaryAsset(group.getVehicle(), data.getVehicle(), "有车产", "无车产"));
        putMatch(fieldMatches, "house", group.getHouse(), describeBinaryAsset(data.getHouse(), "有房产", "无房产"),
                matchBinaryAsset(group.getHouse(), data.getHouse(), "有房产", "无房产"));
        putMatch(fieldMatches, "householdRegister", group.getHouseholdRegister(), describeHouseholdRegister(data),
                matchHouseholdRegister(group.getHouseholdRegister(), data));

        if (fieldMatches.isEmpty()) {
            return new RuleGroupMatchResult(groupName, requireAll, true, fieldMatches);
        }
        boolean matched = requireAll
                ? fieldMatches.values().stream().allMatch(RuleFieldMatch::matched)
                : fieldMatches.values().stream().anyMatch(RuleFieldMatch::matched);
        return new RuleGroupMatchResult(groupName, requireAll, matched, fieldMatches);
    }

    private void putMatch(Map<String, RuleFieldMatch> fieldMatches, String key, List<String> options,
                          String actual, Boolean matched) {
        if (matched != null) {
            fieldMatches.put(key, new RuleFieldMatch(options, actual, matched));
        }
    }

    private String buildQualificationMismatchLog(RuleGroupMatchResult... results) {
        return Arrays.stream(results)
                .filter(Objects::nonNull)
                .filter(result -> !result.matched())
                .map(this::formatRuleGroupMismatch)
                .collect(Collectors.joining("; "));
    }

    private String formatRuleGroupMismatch(RuleGroupMatchResult result) {
        String details = result.fieldMatches().entrySet().stream()
                .filter(entry -> result.requireAll() ? !entry.getValue().matched() : true)
                .map(entry -> formatRuleFieldMismatch(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", "));
        String mode = result.requireAll() ? "全部满足" : "至少满足一项";
        return result.groupName() + "(" + mode + ")未命中[" + details + "]";
    }

    private String formatRuleFieldMismatch(String fieldName, RuleFieldMatch fieldMatch) {
        return fieldName + "{actual=" + fieldMatch.actual() + ", options=" + fieldMatch.options() + "}";
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

    private Boolean matchHouseholdRegister(List<String> options, StandardApplyData data) {
        if (isEmpty(options)) {
            return null;
        }
        String workCity = data == null ? null : data.getWorkCity();
        String cityCode = data == null ? null : data.getCityCode();
        String hukou = extraText(data, "hukou");
        String hukouCity = extraText(data, "hukouCity");
        return options.stream().map(this::normalizeToken).anyMatch(option ->
                matchesHukouType(option, hukou)
                        || StringUtils.equalsIgnoreCase(option, normalizeToken(workCity))
                        || StringUtils.equalsIgnoreCase(option, normalizeToken(cityCode))
                        || StringUtils.equalsIgnoreCase(option, normalizeToken(hukouCity))
                        || StringUtils.containsIgnoreCase(workCity, option)
                        || StringUtils.containsIgnoreCase(hukouCity, option));
    }

    private boolean matchesHukouType(String option, String hukou) {
        if (StringUtils.isBlank(option) || StringUtils.isBlank(hukou)) {
            return false;
        }
        String normalizedHukou = normalizeToken(hukou);
        if (StringUtils.equalsIgnoreCase(option, normalizedHukou)) {
            return true;
        }
        boolean localOption = StringUtils.equalsIgnoreCase(option, "LOCAL")
                || StringUtils.equalsIgnoreCase(option, "LOCAL_HUKOU")
                || "本地".equals(option)
                || "本地户籍".equals(option);
        boolean nonLocalOption = StringUtils.equalsIgnoreCase(option, "NON_LOCAL")
                || StringUtils.equalsIgnoreCase(option, "NON_LOCAL_HUKOU")
                || "外地".equals(option)
                || "非本地".equals(option)
                || "非本地户籍".equals(option);
        return localOption && ("本地".equals(normalizedHukou) || "本地户籍".equals(normalizedHukou))
                || nonLocalOption && ("外地".equals(normalizedHukou)
                || "非本地".equals(normalizedHukou)
                || "非本地户籍".equals(normalizedHukou));
    }

    private String describeProfession(Integer value) {
        if (value == null) {
            return "null";
        }
        return switch (value) {
            case 1 -> "1(上班族)";
            case 2 -> "2(自由职业)";
            case 3 -> "3(私营企业主)";
            case 4 -> "4(公务员/事业单位)";
            default -> String.valueOf(value);
        };
    }

    private String describeOverdue(Integer value) {
        if (value == null) {
            return "null";
        }
        return switch (value) {
            case 1 -> "1(信用良好)";
            case 2 -> "2(当前逾期中)";
            default -> String.valueOf(value);
        };
    }

    private String describeDuration(Integer value) {
        if (value == null) {
            return "null";
        }
        return switch (value) {
            case 0 -> "0(无)";
            case 1 -> "1(6个月以下)";
            case 2 -> "2(6-12个月)";
            case 3 -> "3(12个月以上)";
            default -> value + "个月";
        };
    }

    private String describeBinaryAsset(Integer value, String positiveLabel, String negativeLabel) {
        if (value == null) {
            return "null";
        }
        return switch (value) {
            case 1 -> "1(" + positiveLabel + ")";
            case 2 -> "2(" + negativeLabel + ")";
            default -> String.valueOf(value);
        };
    }

    private String describeAmount(Integer value) {
        return value == null ? "null" : value + "元";
    }

    private String describeMonths(Integer value) {
        return value == null ? "null" : value + "个月";
    }

    private String describePlain(Integer value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private String describeHouseholdRegister(StandardApplyData data) {
        return "cityCode=" + data.getCityCode()
                + ",workCity=" + data.getWorkCity()
                + ",hukou=" + extraText(data, "hukou")
                + ",hukouCity=" + extraText(data, "hukouCity");
    }

    private String extraText(StandardApplyData data, String key) {
        if (data == null || data.getExtraInfo() == null || !data.getExtraInfo().containsKey(key)) {
            return null;
        }
        Object value = data.getExtraInfo().get(key);
        return value == null ? null : String.valueOf(value);
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
        String normalizedCityCode = normalizeCityCode(cityCode);
        return cities.stream().anyMatch(city ->
                matchesCityCode(city, cityCode, normalizedCityCode)
                        || StringUtils.equalsIgnoreCase(city, cityCode)
                        || StringUtils.equalsIgnoreCase(city, workCity)
                        || (StringUtils.isNotBlank(workCity) && workCity.contains(city)));
    }

    private boolean matchesCityCode(String configuredCity, String inputCityCode, String normalizedInputCityCode) {
        String normalizedConfiguredCity = normalizeCityCode(configuredCity);
        if (StringUtils.equalsIgnoreCase(normalizedConfiguredCity, normalizedInputCityCode)) {
            return true;
        }
        if (isLegacyFourDigitCityCode(configuredCity) || isLegacyFourDigitCityCode(inputCityCode)) {
            return StringUtils.equalsIgnoreCase(cityCodePrefix(configuredCity), cityCodePrefix(inputCityCode));
        }
        return false;
    }

    private String normalizeCityCode(String value) {
        String normalized = normalizeToken(value);
        if (StringUtils.isBlank(normalized)) {
            return normalized;
        }
        String digits = normalized.chars()
                .filter(Character::isDigit)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        if (digits.length() >= 6) {
            return toCityLevelCode(digits.substring(0, 6));
        }
        return digits.length() >= 4 ? digits.substring(0, 4) : normalized;
    }

    private String toCityLevelCode(String code) {
        if (code.length() < 6 || "90".equals(code.substring(2, 4))) {
            return code;
        }
        return code.substring(0, 4) + "00";
    }

    private boolean isLegacyFourDigitCityCode(String value) {
        String normalized = normalizeToken(value);
        return StringUtils.isNotBlank(normalized)
                && normalized.length() == 4
                && normalized.chars().allMatch(Character::isDigit);
    }

    private String cityCodePrefix(String value) {
        String normalized = normalizeToken(value);
        if (StringUtils.isBlank(normalized)) {
            return normalized;
        }
        String digits = normalized.chars()
                .filter(Character::isDigit)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        return digits.length() >= 4 ? digits.substring(0, 4) : normalized;
    }

    private boolean isAllCity(List<String> cities) {
        return cities != null && cities.stream()
                .map(this::normalizeToken)
                .anyMatch(city -> CITY_ALL.equalsIgnoreCase(city) || "全国".equals(city));
    }

    private String buildProductIds(List<InstitutionProduct> products) {
        if (products == null || products.isEmpty()) {
            return "[]";
        }
        return products.stream()
                .map(InstitutionProduct::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String buildProductBriefs(List<InstitutionProduct> products) {
        if (products == null || products.isEmpty()) {
            return "[]";
        }
        return products.stream()
                .map(product -> String.format("{productId=%s, productName=%s, instId=%s, priority=%s}",
                        product.getId(), product.getProductName(), product.getInstId(), product.getPriority()))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private record RuleGroupMatchResult(String groupName, boolean requireAll, boolean matched,
                                        Map<String, RuleFieldMatch> fieldMatches) {
    }

    private record RuleFieldMatch(List<String> options, String actual, boolean matched) {
    }
}
