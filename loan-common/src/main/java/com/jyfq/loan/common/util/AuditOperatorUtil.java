package com.jyfq.loan.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolves the current operator for audit fields.
 */
public final class AuditOperatorUtil {

    private static final String DEFAULT_OPERATOR = "system";
    private static final String[] HEADER_CANDIDATES = {
            "X-Operator",
            "X-Operator-Name",
            "X-User",
            "X-User-Name",
            "X-Login-User",
            "operatorName",
            "operator",
            "username"
    };

    private AuditOperatorUtil() {
    }

    public static String currentOperator() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return DEFAULT_OPERATOR;
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        for (String header : HEADER_CANDIDATES) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return DEFAULT_OPERATOR;
    }
}
