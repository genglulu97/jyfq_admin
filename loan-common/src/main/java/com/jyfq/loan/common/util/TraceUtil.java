package com.jyfq.loan.common.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * TraceID 工具类 — 全链路追踪
 */
public final class TraceUtil {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private TraceUtil() {
    }

    /**
     * 生成 TraceID 并放入 MDC
     */
    public static String generate() {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        MDC.put(TRACE_ID_KEY, traceId);
        return traceId;
    }

    /**
     * 设置 TraceID（从上游传入时使用）
     */
    public static void set(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 获取当前 TraceID
     */
    public static String get() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 清除 MDC 中的 TraceID
     */
    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }
}
