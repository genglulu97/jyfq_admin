package com.jyfq.loan.service.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上游渠道策略工厂
 */
@Component
public class UpstreamStrategyFactory {

    private final Map<String, UpstreamStrategy> strategyMap = new ConcurrentHashMap<>();

    public UpstreamStrategyFactory(List<UpstreamStrategy> strategies) {
        for (UpstreamStrategy strategy : strategies) {
            strategyMap.put(strategy.getChannelCode(), strategy);
        }
    }

    /**
     * 根据渠道编码获取对应的处理策略
     */
    public UpstreamStrategy getStrategy(String channelCode) {
        return strategyMap.get(channelCode);
    }
}
