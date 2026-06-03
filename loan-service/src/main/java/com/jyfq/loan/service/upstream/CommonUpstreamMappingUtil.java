package com.jyfq.loan.service.upstream;

import cn.hutool.crypto.digest.DigestUtil;
import com.jyfq.loan.model.dto.CommonUpstreamPayloadDTO;
import com.jyfq.loan.model.dto.StandardApplyData;
import org.springframework.util.StringUtils;

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
                .idCardPrefixFour(trimToNull(payload.getIdCardPrefixFour()))
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
                .commercialInsurance(normalizeCommercialInsurance(payload.getCommercialInsurance()))
                .overdue(normalizeOverdue(payload.getOverdue()))
                .loanAmount(normalizeLoanAmount(payload.getLoanAmount()))
                .loanTime(normalizeLoanTime(payload.getLoanTime()))
                .ip(trimToNull(payload.getDeviceIp()))
                .build();
    }

    public static String resolveCityCode(CommonUpstreamPayloadDTO payload) {
        if (payload == null) {
            return null;
        }
        return trimToNull(payload.getCityCode());
    }

    public static String resolveWorkCity(CommonUpstreamPayloadDTO payload) {
        if (payload == null) {
            return null;
        }
        return trimToNull(payload.getCity());
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
            case 1 -> 620;
            case 2 -> 680;
            case 3 -> 720;
            case 4 -> null;
            case 5 -> 580;
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

    public static Integer normalizeCommercialInsurance(Integer value) {
        if (value == null) {
            return 0;
        }
        return switch (value) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 0;
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

}
