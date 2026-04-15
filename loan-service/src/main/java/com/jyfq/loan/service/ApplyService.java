package com.jyfq.loan.service;

import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.thirdparty.model.PreCheckResult;
import com.jyfq.loan.thirdparty.model.PushResult;

/**
 * 进件申请服务
 */
public interface ApplyService {

    /**
     * 预撞库：并行调用所有匹配到的下游机构，获取出价并筛选最优
     *
     * @param data 标准化用户数据
     * @return 最优的撞库结果（包含最高价及产品信息）
     */
    PreCheckResult competitivePreCheck(StandardApplyData data);

    /**
     * 正式进件：将数据推送到指定的机构
     *
     * @param data    标准化用户数据
     * @param productId 指定产品ID
     * @return 进件结果
     */
    PushResult pushToInstitution(StandardApplyData data, Long productId);
}
