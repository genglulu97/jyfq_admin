package com.jyfq.loan.service.impl;

import com.alibaba.fastjson2.JSON;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.mapper.InstitutionProductMapper;
import com.jyfq.loan.mapper.PushRecordMapper;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.entity.InstitutionProduct;
import com.jyfq.loan.model.entity.PushRecord;
import com.jyfq.loan.service.PushService;
import com.jyfq.loan.thirdparty.InstitutionAdapter;
import com.jyfq.loan.thirdparty.InstitutionAdapterRegistry;
import com.jyfq.loan.thirdparty.model.PushRequest;
import com.jyfq.loan.thirdparty.model.PushResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Push service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushServiceImpl implements PushService {

    private final InstitutionProductMapper institutionProductMapper;
    private final InstitutionMapper institutionMapper;
    private final InstitutionAdapterRegistry adapterRegistry;
    private final PushRecordMapper pushRecordMapper;
    @SuppressWarnings("unused")
    private final ApplyOrderMapper applyOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PushResult executePush(StandardApplyData data, Long productId) {
        log.info("[PUSH] execute push, productId={}, phoneMd5={}", productId, data.getPhoneMd5());

        InstitutionProduct product = institutionProductMapper.selectById(productId);
        if (product == null) {
            return PushResult.failure("Product not found: " + productId);
        }

        Institution inst = institutionMapper.selectById(product.getInstId());
        if (inst == null) {
            return PushResult.failure("Institution not found: " + product.getInstId());
        }

        InstitutionAdapter adapter = adapterRegistry.getAdapter(inst.getInstCode());
        if (adapter == null) {
            return PushResult.failure("Adapter not found: " + inst.getInstCode());
        }

        String traceId = UUID.randomUUID().toString().replace("-", "");

        PushRequest pushReq = new PushRequest();
        pushReq.setOrderNo("P" + System.currentTimeMillis());
        pushReq.setTraceId(traceId);
        pushReq.setProductId(productId);
        pushReq.setInstCode(inst.getInstCode());
        pushReq.setNotifyUrl(inst.getApiNotifyUrl());
        pushReq.setStandardData(data);

        long start = System.currentTimeMillis();
        PushResult result = adapter.push(pushReq);
        long cost = System.currentTimeMillis() - start;

        PushRecord record = new PushRecord();
        record.setOrderNo(pushReq.getOrderNo());
        record.setInstId(inst.getId());
        record.setInstCode(inst.getInstCode());
        record.setProductId(product.getId());
        record.setTraceId(traceId);
        record.setThirdOrderNo(result.getThirdOrderNo());
        record.setPushStatus(result.isSuccess() ? 2 : 4);
        record.setResponseLog(JSON.toJSONString(result));
        record.setErrorMsg(result.getErrorMsg());
        record.setCostMs((int) cost);
        record.setPushedAt(LocalDateTime.now());
        pushRecordMapper.insert(record);

        return result;
    }
}
