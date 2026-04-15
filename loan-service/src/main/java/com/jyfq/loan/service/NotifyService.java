package com.jyfq.loan.service;

import com.jyfq.loan.model.enums.NotifyStatus;

/**
 * 回调通知处理服务
 */
public interface NotifyService {

    /**
     * 处理机构回调通知（幂等 + 分布式锁）
     *
     * @param instCode 机构编码
     * @param notifyNo 机构方流水号
     * @param orderNo  业务单号
     * @param status   回调状态
     * @param rawBody  原始回调报文
     */
    void handleNotify(String instCode, String notifyNo, String orderNo, NotifyStatus status, String rawBody);
}
