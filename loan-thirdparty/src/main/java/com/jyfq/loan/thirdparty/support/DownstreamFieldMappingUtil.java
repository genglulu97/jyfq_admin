package com.jyfq.loan.thirdparty.support;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Shared helpers for mapping standardized apply fields to downstream-specific values.
 */
public final class DownstreamFieldMappingUtil {

    private DownstreamFieldMappingUtil() {
    }

    public static void putIfNotNull(JSONObject payload, String key, Object value) {
        if (payload != null && value != null) {
            payload.put(key, value);
        }
    }

    public static String mapFromDict(Integer sourceValue, Map<Integer, String> mapping, String defaultValue) {
        if (sourceValue == null || mapping == null || mapping.isEmpty()) {
            return defaultValue;
        }
        return mapping.getOrDefault(sourceValue, defaultValue);
    }

    public static String genderText(Integer gender) {
        if (gender == null) {
            return null;
        }
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            default -> null;
        };
    }

    public static String professionText(Integer profession) {
        if (profession == null) {
            return null;
        }
        return switch (profession) {
            case 1 -> "上班族";
            case 2 -> "自由职业";
            case 3 -> "企业主";
            case 4 -> "公职人员";
            default -> null;
        };
    }

    public static String binaryAssetText(Integer value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case 1 -> "有";
            case 2 -> "无";
            default -> null;
        };
    }

    public static String overdueText(Integer overdue) {
        if (overdue == null) {
            return null;
        }
        return switch (overdue) {
            case 1 -> "无逾期";
            case 2 -> "有逾期";
            default -> null;
        };
    }

    public static String durationText(Integer durationCode) {
        if (durationCode == null) {
            return null;
        }
        return switch (durationCode) {
            case 0 -> "无";
            case 1 -> "6个月以下";
            case 2 -> "6-12个月";
            case 3 -> "12个月以上";
            default -> null;
        };
    }

    public static String zhimaBucket(Integer zhima) {
        if (zhima == null) {
            return null;
        }
        if (zhima < 600) {
            return "600以下";
        }
        if (zhima < 650) {
            return "600-650";
        }
        if (zhima < 700) {
            return "650-700";
        }
        return "700以上";
    }

    public static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
