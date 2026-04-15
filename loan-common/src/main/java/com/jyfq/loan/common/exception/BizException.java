package com.jyfq.loan.common.exception;

import com.jyfq.loan.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常（可控异常，会被全局拦截器捕获并返回友好信息）
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String detail) {
        super(resultCode.getMessage() + ": " + detail);
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
