package com.jyfq.loan.common.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Time-slot validation helper.
 */
@Slf4j
public final class TimeUtil {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private TimeUtil() {
    }

    /**
     * Supports either [{"start":"09:00","end":"18:00"}]
     * or [{"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"18:00"}].
     */
    public static boolean isCurrentInSlots(String slotsJson) {
        if (StringUtils.isBlank(slotsJson)) {
            return true;
        }

        try {
            JSONArray slots = JSON.parseArray(slotsJson);
            LocalTime now = LocalTime.now();
            DayOfWeek currentDay = LocalDate.now().getDayOfWeek();

            for (int i = 0; i < slots.size(); i++) {
                JSONObject slot = slots.getJSONObject(i);
                String dayValue = firstNonBlank(slot.getString("dayOfWeek"), slot.getString("day"), slot.getString("weekday"));
                if (StringUtils.isNotBlank(dayValue) && !matchesDay(dayValue, currentDay)) {
                    continue;
                }
                String startStr = firstNonBlank(slot.getString("start"), slot.getString("startTime"));
                String endStr = firstNonBlank(slot.getString("end"), slot.getString("endTime"));
                if (StringUtils.isBlank(startStr) || StringUtils.isBlank(endStr)) {
                    continue;
                }

                LocalTime start = LocalTime.parse(startStr, HH_MM);
                LocalTime end = LocalTime.parse(endStr, HH_MM);
                if (!now.isBefore(start) && !now.isAfter(end)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            log.error("Time slot parse error: {}", slotsJson, ex);
            return true;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean matchesDay(String dayValue, DayOfWeek currentDay) {
        String normalized = dayValue.trim().toUpperCase();
        if (normalized.startsWith("周")) {
            return switch (normalized) {
                case "周一" -> currentDay == DayOfWeek.MONDAY;
                case "周二" -> currentDay == DayOfWeek.TUESDAY;
                case "周三" -> currentDay == DayOfWeek.WEDNESDAY;
                case "周四" -> currentDay == DayOfWeek.THURSDAY;
                case "周五" -> currentDay == DayOfWeek.FRIDAY;
                case "周六" -> currentDay == DayOfWeek.SATURDAY;
                case "周日", "周天" -> currentDay == DayOfWeek.SUNDAY;
                default -> true;
            };
        }
        return switch (normalized) {
            case "MONDAY" -> currentDay == DayOfWeek.MONDAY;
            case "TUESDAY" -> currentDay == DayOfWeek.TUESDAY;
            case "WEDNESDAY" -> currentDay == DayOfWeek.WEDNESDAY;
            case "THURSDAY" -> currentDay == DayOfWeek.THURSDAY;
            case "FRIDAY" -> currentDay == DayOfWeek.FRIDAY;
            case "SATURDAY" -> currentDay == DayOfWeek.SATURDAY;
            case "SUNDAY" -> currentDay == DayOfWeek.SUNDAY;
            default -> true;
        };
    }
}
