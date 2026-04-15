package com.jyfq.loan.service;

import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.thirdparty.model.PushResult;

/**
 * 推单服务接口
 */
public interface PushService {

    /**
     * 执行正式推单给第三方机构
     */
    PushResult executePush(StandardApplyData data, Long productId);
}