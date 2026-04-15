package com.jyfq.loan.service.strategy;

import com.jyfq.loan.model.dto.StandardApplyData;

/**
 * 上游渠道策略接口 (处理不同渠道的加解密及数据标准化)
 */
public interface UpstreamStrategy {

    /**
     * 获取渠道编码
     */
    String getChannelCode();

    /**
     * 解析并脱敏上游请求数据，转换为内部标准模型
     *
     * @param rawData 原始请求数据 (通常是密文)
     * @return 标准化数据模型
     */
    StandardApplyData parseRequest(String rawData);

    /**
     * 将内部结果包装并加密为上游要求的格式
     *
     * @param result 内部处理结果
     * @return 加密后的响应内容
     */
    String encryptResponse(Object result);
}
