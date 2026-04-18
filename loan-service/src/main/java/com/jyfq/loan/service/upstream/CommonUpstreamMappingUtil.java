package com.jyfq.loan.service.upstream;

import cn.hutool.crypto.digest.DigestUtil;
import com.jyfq.loan.model.dto.CommonUpstreamPayloadDTO;
import com.jyfq.loan.model.dto.StandardApplyData;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Common upstream public protocol enum mapping.
 */
public final class CommonUpstreamMappingUtil {

    private CommonUpstreamMappingUtil() {
    }

    public static StandardApplyData toStandardData(String channelCode, CommonUpstreamPayloadDTO payload) {
        String workCity = resolveWorkCity(payload);
        String phone = trimToNull(payload.getPhone());
        return StandardApplyData.builder()
                .channelCode(channelCode)
                .name(trimToNull(payload.getName()))
                .phone(phone)
                .phoneMd5(resolvePhoneMd5(payload.getPhoneMd5(), phone))
                .idCard(trimToNull(payload.getIdCard()))
                .age(payload.getAge())
                .cityCode(resolveCityCode(payload))
                .workCity(workCity)
                .gender(normalizeGender(payload.getGender()))
                .profession(normalizeProfession(payload.getProfession()))
                .zhima(normalizeZhima(payload.getZhima()))
                .house(normalizeBinaryAsset(payload.getHouse()))
                .vehicle(normalizeBinaryAsset(payload.getVehicle()))
                .providentFund(normalizeDuration(payload.getProvidentFund()))
                .socialSecurity(normalizeDuration(payload.getSocialSecurity()))
                .commercialInsurance(normalizeDuration(payload.getCommercialInsurance()))
                .overdue(normalizeOverdue(payload.getOverdue()))
                .loanAmount(normalizeLoanAmount(payload.getLoanAmount()))
                .loanTime(normalizeLoanTime(payload.getLoanTime()))
                .customerLevel(trimToNull(payload.getCustomerLevel()))
                .ip(trimToNull(payload.getDeviceIp()))
                .extraInfo(buildExtraInfo(payload))
                .build();
    }

    public static String resolveCityCode(CommonUpstreamPayloadDTO payload) {
        if (payload == null) {
            return null;
        }
        if (StringUtils.hasText(payload.getCityCode())) {
            return payload.getCityCode().trim();
        }
        return trimToNull(payload.getCity());
    }

    public static String resolveWorkCity(CommonUpstreamPayloadDTO payload) {
        if (payload == null) {
            return null;
        }
        if (StringUtils.hasText(payload.getWorkCity())) {
            return payload.getWorkCity().trim();
        }
        if (StringUtils.hasText(payload.getProvince()) && StringUtils.hasText(payload.getCity())) {
            return payload.getProvince().trim() + "/" + payload.getCity().trim();
        }
        if (StringUtils.hasText(payload.getCity())) {
            return payload.getCity().trim();
        }
        return null;
    }

    public static String resolvePhoneMd5(String phoneMd5, String phone) {
        if (StringUtils.hasText(phoneMd5)) {
            return phoneMd5.trim().toLowerCase();
        }
        return StringUtils.hasText(phone) ? DigestUtil.md5Hex(phone.trim()) : null;
    }

    public static Integer normalizeGender(Integer value) {
        if (value == null) {
            return 0;
        }
        return switch (value) {
            case 1, 2 -> value;
            default -> 0;
        };
    }

    public static Integer normalizeProfession(Integer value) {
        if (value == null) {
            return 0;
        }
        return switch (value) {
            case 1, 2, 3, 4 -> value;
            default -> 0;
        };
    }

    public static Integer normalizeZhima(Integer value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case 1 -> 580;
            case 2 -> 620;
            case 3 -> 680;
            case 4 -> 720;
            case 5 -> null;
            default -> value > 100 ? value : null;
        };
    }

    public static Integer normalizeDuration(Integer value) {
        if (value == null) {
            return 0;
        }
        return switch (value) {
            case 1, 2, 3 -> value;
            case 0, 4 -> 0;
            default -> 0;
        };
    }

    public static Integer normalizeBinaryAsset(Integer value) {
        if (value == null) {
            return 0;
        }
        return switch (value) {
            case 1, 2 -> value;
            default -> 0;
        };
    }

    public static Integer normalizeOverdue(Integer value) {
        if (value == null) {
            return 0;
        }
        return switch (value) {
            case 1, 2 -> value;
            default -> 0;
        };
    }

    public static Integer normalizeLoanAmount(Integer value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case 1 -> 30000;
            case 2 -> 50000;
            case 3 -> 100000;
            case 4 -> 200000;
            default -> value;
        };
    }

    public static Integer normalizeLoanTime(Integer value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case 2 -> 6;
            case 3 -> 12;
            case 4 -> 24;
            case 5 -> 36;
            default -> value;
        };
    }

    public static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static Map<String, Object> buildExtraInfo(CommonUpstreamPayloadDTO payload) {
        Map<String, Object> extraInfo = new LinkedHashMap<>();
        if (payload.getExtraInfo() != null && !payload.getExtraInfo().isEmpty()) {
            extraInfo.putAll(payload.getExtraInfo());
        }
        putIfPresent(extraInfo, "city", trimToNull(payload.getCity()));
        putIfPresent(extraInfo, "province", trimToNull(payload.getProvince()));
        putIfPresent(extraInfo, "provinceCode", trimToNull(payload.getProvinceCode()));
        putIfPresent(extraInfo, "agreeProtocol", trimToNull(payload.getAgreeProtocol()));
        if (payload.getProductId() != null) {
            extraInfo.put("productId", payload.getProductId());
        }
        return extraInfo.isEmpty() ? null : extraInfo;
    }

    private static void putIfPresent(Map<String, Object> extraInfo, String key, String value) {
        if (StringUtils.hasText(value)) {
            extraInfo.put(key, value);
        }
    }
}
