package com.jyfq.loan.thirdparty;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Institution adapter registry.
 */
@Component
public class InstitutionAdapterRegistry {

    private final Map<String, InstitutionAdapter> adapterMap = new ConcurrentHashMap<>();

    public InstitutionAdapterRegistry(Map<String, InstitutionAdapter> adapters) {
        for (Map.Entry<String, InstitutionAdapter> entry : adapters.entrySet()) {
            register(entry.getKey(), entry.getValue());
            register(entry.getValue().getAdapterKey(), entry.getValue());
        }
    }

    public InstitutionAdapter getAdapter(String adapterKey) {
        if (!StringUtils.hasText(adapterKey)) {
            return null;
        }
        return adapterMap.get(adapterKey.trim().toLowerCase());
    }

    private void register(String key, InstitutionAdapter adapter) {
        if (StringUtils.hasText(key) && adapter != null) {
            adapterMap.put(key.trim().toLowerCase(), adapter);
        }
    }
}
