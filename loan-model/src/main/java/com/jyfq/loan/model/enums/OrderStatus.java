package com.jyfq.loan.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    PENDING(0, "待处理"),
    PUSHING(1, "推单中"),
    CREDITING(2, "授信中"),
    LOANED(3, "已放款"),
    FAILED(9, "失败");

    private final int code;
    private final String desc;

    public static OrderStatus of(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知订单状态: " + code);
    }
}
