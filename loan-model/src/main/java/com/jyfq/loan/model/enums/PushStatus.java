package com.jyfq.loan.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 推单状态枚举
 */
@Getter
@AllArgsConstructor
public enum PushStatus {

    PENDING(0, "待推"),
    PUSHING(1, "推送中"),
    RECEIVED(2, "已接收"),
    APPROVED(3, "授信通过"),
    REJECTED(4, "拒绝"),
    TIMEOUT(9, "超时");

    private final int code;
    private final String desc;

    public static PushStatus of(int code) {
        for (PushStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知推单状态: " + code);
    }
}
