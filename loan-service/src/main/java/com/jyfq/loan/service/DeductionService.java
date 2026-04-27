package com.jyfq.loan.service;

import com.jyfq.loan.model.enums.NotifyStatus;

/**
 * 计费扣费服务
 */
public interface DeductionService {

    /**
     * 创建扣费记录
     *
     * @param orderNo  业务单号
     * @param instCode 机构编码
     * @param status   触发扣费的状态（授信通过/放款成功）
     */
    void createDeduction(String orderNo, String instCode, NotifyStatus status);

    /**
     * Create institution deduction after downstream apply succeeds.
     *
     * @param orderNo local business order number
     */
    void createPushSuccessDeduction(String orderNo);
}
