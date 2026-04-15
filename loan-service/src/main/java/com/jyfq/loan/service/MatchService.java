package com.jyfq.loan.service;

import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.model.entity.InstitutionProduct;

import java.util.List;

/**
 * 撞库匹配服务
 */
public interface MatchService {

    /**
     * 基础规则匹配：根据年龄、城市、营业时间、资质、渠道黑名单筛选可用产品
     *
     * @param data 标准申请数据
     * @return 匹配成功的产品列表（已按优先级排序）
     */
    List<InstitutionProduct> findMatchedProducts(StandardApplyData data);
}
