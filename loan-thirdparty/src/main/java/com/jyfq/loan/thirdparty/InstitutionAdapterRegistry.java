package com.jyfq.loan.thirdparty;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机构适配器注册表
 */
@Component
public class InstitutionAdapterRegistry {

    private final Map<String, InstitutionAdapter> adapterMap = new ConcurrentHashMap<>();

    public InstitutionAdapterRegistry(List<InstitutionAdapter> adapters) {
        // 自动注册所有 Spring 管理的 Adapter 实例
        for (InstitutionAdapter adapter : adapters) {
            adapterMap.put(adapter.getInstCode(), adapter);
        }
    }

    /**
     * 根据机构编码获取适配器
     *
     * @param instCode 机构编码
     * @return 适配器实例
     */
    public InstitutionAdapter getAdapter(String instCode) {
        return adapterMap.get(instCode);
    }
}
