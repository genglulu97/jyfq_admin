package com.jyfq.loan.common.exception;

import lombok.Getter;

/**
 * 第三方接口调用异常
 */
@Getter
public class ThirdPartyException extends RuntimeException {

    private final String instCode;

    public ThirdPartyException(String instCode, Throwable cause) {
        super("第三方机构[" + instCode + "]调用异常: " + cause.getMessage(), cause);
        this.instCode = instCode;
    }

    public ThirdPartyException(String instCode, String message) {
        super("第三方机构[" + instCode + "]调用异常: " + message);
        this.instCode = instCode;
    }
}
