package com.jyfq.loan.common.config;

import com.jyfq.loan.common.util.TraceUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * TraceID 过滤器
 * <p>每个请求注入 TraceID，支持上游透传</p>
 */
@Component
@Order(1)
public class TraceIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String traceId = httpRequest.getHeader(TraceUtil.TRACE_ID_HEADER);

            if (StringUtils.hasText(traceId)) {
                TraceUtil.set(traceId);
            } else {
                TraceUtil.generate();
            }

            chain.doFilter(request, response);
        } finally {
            TraceUtil.clear();
        }
    }
}
