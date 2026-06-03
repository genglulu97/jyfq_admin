package com.jyfq.loan.app.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.List;

public final class ClientIpUtil {

    private static final List<String> IP_HEADERS = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    );

    private ClientIpUtil() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        for (String header : IP_HEADERS) {
            String value = firstIp(request.getHeader(header));
            if (isUsableIp(value)) {
                return value;
            }
        }
        return request.getRemoteAddr();
    }

    private static String firstIp(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.split(",")[0].trim();
    }

    private static boolean isUsableIp(String value) {
        return StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value.trim());
    }
}
