package com.jyfq.loan.thirdparty.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推单结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushResult {

    /** 是否成功 */
    private boolean success;

    /** 机构方流水号 */
    private String thirdOrderNo;

    /** 机构编码 */
    private String instCode;

    /** 错误信息 */
    private String errorMsg;

    /** 接口耗时ms */
    private long costMs;

    public String getMsg() {
        return errorMsg;
    }

    public void setMsg(String msg) {
        this.errorMsg = msg;
    }

    public static PushResult success(String msg) {
        PushResult result = new PushResult();
        result.setSuccess(true);
        result.setErrorMsg(msg);
        return result;
    }

    public static PushResult failure(String msg) {
        PushResult result = new PushResult();
        result.setSuccess(false);
        result.setErrorMsg(msg);
        return result;
    }
}
