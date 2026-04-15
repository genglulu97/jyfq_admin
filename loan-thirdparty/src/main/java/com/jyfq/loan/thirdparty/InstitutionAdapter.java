package com.jyfq.loan.thirdparty;

import com.jyfq.loan.thirdparty.model.PreCheckRequest;
import com.jyfq.loan.thirdparty.model.PreCheckResult;
import com.jyfq.loan.thirdparty.model.PushRequest;
import com.jyfq.loan.thirdparty.model.PushResult;

/**
 * 第三方机构统一适配接口
 * <p>所有机构都实现这个接口</p>
 */
public interface InstitutionAdapter {

    /**
     * 获取机构编码
     */
    String getInstCode();

    /**
     * 预授信
     */
    PreCheckResult preCheck(PreCheckRequest req);

    /**
     * 正式推单
     */
    PushResult push(PushRequest req);
}
