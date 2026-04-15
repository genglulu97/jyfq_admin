package com.jyfq.loan.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 回调通知状态枚举
 */
@Getter
@AllArgsConstructor
public enum NotifyStatus {

    APPROVE(1, "授信通过"),
    REJECT(2, "授信拒绝"),
    LOAN(3, "放款成功"),
    LOAN_FAIL(4, "放款失败");

    private final int code;
    private final String desc;

    /**
     * 转换为推单状态
     */
    public PushStatus toPushStatus() {
        return switch (this) {
            case APPROVE -> PushStatus.APPROVED;
            case REJECT  -> PushStatus.REJECTED;
            case LOAN    -> PushStatus.APPROVED;
            case LOAN_FAIL -> PushStatus.REJECTED;
        };
    }

    public static NotifyStatus of(int code) {
        for (NotifyStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知通知状态: " + code);
    }
}
