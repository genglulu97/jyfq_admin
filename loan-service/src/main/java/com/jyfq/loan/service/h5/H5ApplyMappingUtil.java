package com.jyfq.loan.service.h5;

import cn.hutool.crypto.digest.DigestUtil;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.ResultCode;
import com.jyfq.loan.model.dto.H5ApplyRequestDTO;
import com.jyfq.loan.model.dto.StandardApplyData;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts public H5 form values into the internal matching model.
 */
public final class H5ApplyMappingUtil {

    private H5ApplyMappingUtil() {
    }

    public static StandardApplyData toStandardData(H5ApplyRequestDTO request, String clientIp) {
        if (request == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "request is required");
        }

        String phone = requiredText(request.getPhone(), "phone");
        Integer socialSecurity = normalizeSocialSecurity(request.getSocialSec(), request.getSocialSecSupply());
        CarHouse carHouse = normalizeCarHouse(request.getCarHouse());

        return StandardApplyData.builder()
                .channelCode(requiredText(request.getChannelCode(), "channelCode"))
                .phone(phone)
                .phoneMd5(DigestUtil.md5Hex(phone))
                .age(normalizeAge(request.getAge()))
                .cityCode(requiredText(request.getCity(), "city"))
                .workCity(requiredText(request.getCity(), "city"))
                .gender(0)
                .profession(normalizeOccupation(request.getOccupation()))
                .zhima(normalizeZhima(request.getZhima()))
                .house(carHouse.house())
                .vehicle(carHouse.vehicle())
                .providentFund(normalizeYesNoDuration(request.getGongjijin(), "gongjijin"))
                .socialSecurity(socialSecurity)
                .commercialInsurance(normalizeCommercialInsurance(request.getAssets()))
                .overdue(1)
                .loanAmount(normalizeLoanAmount(request.getAmount()))
                .loanTime(resolveLoanTime(request.getLoanTime()))
                .ip(firstText(request.getDeviceIp(), clientIp))
                .extraInfo(buildExtraInfo(request))
                .build();
    }

    private static Map<String, Object> buildExtraInfo(H5ApplyRequestDTO request) {
        Map<String, Object> extraInfo = new LinkedHashMap<>();
        extraInfo.put("source", "H5");
        extraInfo.put("ageRange", trimToNull(request.getAge()));
        extraInfo.put("occupation", trimToNull(request.getOccupation()));
        extraInfo.put("city", trimToNull(request.getCity()));
        extraInfo.put("socialSec", trimToNull(request.getSocialSec()));
        extraInfo.put("socialSecSupply", trimToNull(request.getSocialSecSupply()));
        extraInfo.put("hukou", trimToNull(request.getHukou()));
        extraInfo.put("hukouCity", trimToNull(request.getHukouCity()));
        extraInfo.put("zhimaRange", trimToNull(request.getZhima()));
        extraInfo.put("gongjijin", trimToNull(request.getGongjijin()));
        extraInfo.put("carHouse", trimToNull(request.getCarHouse()));
        extraInfo.put("assets", normalizeAssets(request.getAssets()));
        if (request.getAmount() != null) {
            extraInfo.put("amount", request.getAmount());
        }
        if (request.getLoanTime() != null) {
            extraInfo.put("loanTime", request.getLoanTime());
        }
        return extraInfo;
    }

    private static Integer normalizeAge(String value) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw missing("age");
        }
        Integer explicit = parseInteger(normalized);
        if (explicit != null) {
            return explicit;
        }
        if (containsAny(normalized, "22岁以下", "22以下", "22岁以内")) {
            return 21;
        }
        if (containsRange(normalized, 22, 30)) {
            return 26;
        }
        if (containsRange(normalized, 31, 40)) {
            return 35;
        }
        if (containsRange(normalized, 41, 50)) {
            return 45;
        }
        if (containsAny(normalized, "50岁以上", "50以上", "大于50")) {
            return 55;
        }
        throw invalid("age", value);
    }

    private static Integer normalizeOccupation(String value) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw missing("occupation");
        }
        Integer explicit = parseInteger(normalized);
        if (explicit != null && explicit >= 1 && explicit <= 4) {
            return explicit;
        }
        if (containsAny(normalized, "上班族", "上班", "员工", "EMPLOYEE")) {
            return 1;
        }
        if (containsAny(normalized, "自由职业", "自由", "SELFEMPLOYED", "FREELANCER")) {
            return 2;
        }
        if (containsAny(normalized, "个体经营", "个体户", "企业主", "BUSINESSOWNER")) {
            return 3;
        }
        if (containsAny(normalized, "公务员", "公职", "事业单位", "PUBLICSERVANT")) {
            return 4;
        }
        throw invalid("occupation", value);
    }

    private static Integer normalizeSocialSecurity(String socialSec, String socialSecSupply) {
        Integer duration = normalizeDuration(socialSec, "socialSec");
        if (duration != null && duration > 0) {
            return duration;
        }
        if (isYes(socialSecSupply)) {
            return 1;
        }
        if (isNo(socialSecSupply)) {
            return 0;
        }
        throw invalid("socialSecSupply", socialSecSupply);
    }

    private static Integer normalizeYesNoDuration(String value, String fieldName) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw missing(fieldName);
        }
        if (isYes(normalized)) {
            return 1;
        }
        if (isNo(normalized)) {
            return 0;
        }
        return normalizeDuration(normalized, fieldName);
    }

    private static Integer normalizeDuration(String value, String fieldName) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw missing(fieldName);
        }
        Integer explicit = parseInteger(normalized);
        if (explicit != null) {
            if (explicit <= 0) {
                return 0;
            }
            if (explicit < 6) {
                return 1;
            }
            if (explicit < 12) {
                return 2;
            }
            return 3;
        }
        if (isNo(normalized) || containsAny(normalized, "无社保", "无公积金", "无")) {
            return 0;
        }
        if (containsAny(normalized, "6个月以下", "6月以下", "小于6个月", "少于6个月")) {
            return 1;
        }
        if (containsAny(normalized, "6-12个月", "6~12个月", "6至12个月", "6到12个月")) {
            return 2;
        }
        if (containsAny(normalized, "1年以上", "12个月以上", "12月以上", "一年以上")) {
            return 3;
        }
        if (isYes(normalized)) {
            return 1;
        }
        throw invalid(fieldName, value);
    }

    private static Integer normalizeZhima(String value) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw missing("zhima");
        }
        Integer explicit = parseInteger(normalized);
        if (explicit != null && explicit > 100) {
            return explicit;
        }
        if (containsAny(normalized, "350以下", "低于350")) {
            return 300;
        }
        if (containsRange(normalized, 350, 550)) {
            return 500;
        }
        if (containsRange(normalized, 550, 650)) {
            return 600;
        }
        if (containsRange(normalized, 650, 700)) {
            return 680;
        }
        if (containsAny(normalized, "700以上", "700+", "高于700")) {
            return 720;
        }
        throw invalid("zhima", value);
    }

    private static CarHouse normalizeCarHouse(String value) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            throw missing("carHouse");
        }
        if (containsAny(normalized, "有车有房", "车房都有")) {
            return new CarHouse(1, 1);
        }
        if (containsAny(normalized, "有车")) {
            return new CarHouse(2, 1);
        }
        if (containsAny(normalized, "有房")) {
            return new CarHouse(1, 2);
        }
        if (containsAny(normalized, "无车无房", "无")) {
            return new CarHouse(2, 2);
        }
        throw invalid("carHouse", value);
    }

    private static Integer normalizeCommercialInsurance(List<String> assets) {
        List<String> normalizedAssets = normalizeAssets(assets);
        return normalizedAssets.stream().anyMatch(asset -> asset.contains("商业保险")) ? 1 : 0;
    }

    private static List<String> normalizeAssets(List<String> assets) {
        if (assets == null || assets.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String asset : assets) {
            String trimmed = trimToNull(asset);
            if (trimmed != null) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static Integer resolveLoanTime(Integer loanTime) {
        return loanTime == null || loanTime <= 0 ? 12 : loanTime;
    }

    private static Integer normalizeLoanAmount(String value) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (containsAny(normalized, "1万以下", "1W以下", "小于1万")) {
            return 10000;
        }
        if (containsAny(normalized, "1-5万", "1~5万", "1至5万", "1到5万", "1-5W", "1~5W")) {
            return 50000;
        }
        if (containsAny(normalized, "5-10万", "5~10万", "5至10万", "5到10万", "5-10W", "5~10W")) {
            return 100000;
        }
        if (containsAny(normalized, "10-20万", "10~20万", "10至20万", "10到20万", "10-20W", "10~20W")) {
            return 200000;
        }
        if (containsAny(normalized, "20万以上", "20W以上", "大于20万")) {
            return 200000;
        }
        Integer explicit = parseAmountNumber(normalized);
        if (explicit != null && explicit > 0) {
            return explicit;
        }
        throw invalid("amount", value);
    }

    private static String requiredText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw missing(fieldName);
        }
        return trimmed;
    }

    private static boolean containsRange(String value, int left, int right) {
        return value.contains(left + "-" + right)
                || value.contains(left + "~" + right)
                || value.contains(left + "至" + right)
                || value.contains(left + "到" + right);
    }

    private static boolean containsAny(String value, String... options) {
        if (value == null) {
            return false;
        }
        for (String option : options) {
            if (value.contains(normalize(option))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isYes(String value) {
        String normalized = normalize(value);
        return "有".equals(normalized)
                || "是".equals(normalized)
                || "YES".equals(normalized)
                || "Y".equals(normalized)
                || "TRUE".equals(normalized)
                || "1".equals(normalized);
    }

    private static boolean isNo(String value) {
        String normalized = normalize(value);
        return "无".equals(normalized)
                || "否".equals(normalized)
                || "NO".equals(normalized)
                || "N".equals(normalized)
                || "FALSE".equals(normalized)
                || "0".equals(normalized);
    }

    private static String firstText(String first, String second) {
        String firstText = trimToNull(first);
        return firstText == null ? trimToNull(second) : firstText;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.replace(" ", "")
                .replace("　", "")
                .toUpperCase(Locale.ROOT);
    }

    private static Integer parseInteger(String value) {
        if (!StringUtils.hasText(value) || !value.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private static Integer parseAmountNumber(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String digits = value.chars()
                .filter(Character::isDigit)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        if (!StringUtils.hasText(digits)) {
            return null;
        }
        Integer number = Integer.parseInt(digits);
        if (value.contains("-") || value.contains("~") || value.contains("至") || value.contains("到")) {
            int half = digits.length() / 2;
            if (half > 0 && digits.length() % 2 == 0) {
                number = Integer.parseInt(digits.substring(half));
            }
        }
        if (value.contains("万") || value.contains("W")) {
            return number * 10000;
        }
        return number;
    }

    private static BizException missing(String fieldName) {
        return new BizException(ResultCode.PARAM_MISSING, fieldName);
    }

    private static BizException invalid(String fieldName, String value) {
        return new BizException(ResultCode.PARAM_ERROR, fieldName + "=" + value);
    }

    private record CarHouse(Integer house, Integer vehicle) {
    }
}
