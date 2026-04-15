package com.jyfq.loan.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    // ========== 参数相关 1001~1099 ==========
    PARAM_ERROR(1001, "参数校验失败"),
    PARAM_MISSING(1002, "必填参数缺失"),

    // ========== 认证/权限 2001~2099 ==========
    UNAUTHORIZED(2001, "未登录或登录已过期"),
    FORBIDDEN(2002, "无权限访问"),
    SIGN_ERROR(2003, "签名验证失败"),
    IP_BLOCKED(2004, "IP不在白名单"),

    // ========== 业务异常 3001~3099 ==========
    CHANNEL_NOT_FOUND(3001, "渠道不存在"),
    CHANNEL_DISABLED(3002, "渠道已停用"),
    INST_NOT_FOUND(3003, "机构不存在"),
    INST_DISABLED(3004, "机构已停用"),
    ORDER_NOT_FOUND(3005, "订单不存在"),
    ORDER_STATUS_ERROR(3006, "订单状态异常"),
    FREQ_LIMIT(3007, "请求过于频繁，请稍后再试"),
    CONCURRENT_NOTIFY(3008, "回调并发冲突，请重试"),
    QUOTA_EXCEEDED(3009, "机构日推单配额已满"),
    DUPLICATE_APPLY(3010, "重复进件"),

    // ========== 第三方调用 4001~4099 ==========
    THIRD_PARTY_ERROR(4001, "第三方接口调用失败"),
    THIRD_PARTY_TIMEOUT(4002, "第三方接口调用超时"),
    DECRYPT_ERROR(4003, "报文解密失败"),

    // ========== 系统异常 9001~9099 ==========
    SYSTEM_ERROR(9001, "系统内部异常"),
    DB_ERROR(9002, "数据库异常"),
    REDIS_ERROR(9003, "缓存服务异常"),
    MQ_ERROR(9004, "消息队列异常");

    private final int code;
    private final String message;
}
