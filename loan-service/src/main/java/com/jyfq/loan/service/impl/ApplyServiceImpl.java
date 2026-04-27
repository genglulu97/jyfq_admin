package com.jyfq.loan.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.jyfq.loan.mapper.CollisionRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jyfq.loan.common.util.AesUtil;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.mapper.InstitutionCustomerMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.mapper.InstitutionProductMapper;
import com.jyfq.loan.mapper.PushRecordMapper;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.model.entity.ApplyOrder;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.entity.CollisionRecord;
import com.jyfq.loan.model.entity.InstitutionCustomer;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.entity.InstitutionProduct;
import com.jyfq.loan.model.entity.PushRecord;
import com.jyfq.loan.service.ApplyService;
import com.jyfq.loan.service.DeductionService;
import com.jyfq.loan.service.MatchService;
import com.jyfq.loan.thirdparty.InstitutionAdapter;
import com.jyfq.loan.thirdparty.InstitutionAdapterRegistry;
import com.jyfq.loan.thirdparty.model.PreCheckRequest;
import com.jyfq.loan.thirdparty.model.PreCheckResult;
import com.jyfq.loan.thirdparty.model.PushRequest;
import com.jyfq.loan.thirdparty.model.PushResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Application service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyServiceImpl implements ApplyService {

    private final MatchService matchService;
    private final InstitutionAdapterRegistry adapterRegistry;
    private final InstitutionMapper institutionMapper;
    private final ApplyOrderMapper applyOrderMapper;
    private final CollisionRecordMapper collisionRecordMapper;
    private final ChannelMapper channelMapper;
    private final PushRecordMapper pushRecordMapper;
    private final InstitutionProductMapper institutionProductMapper;
    private final InstitutionCustomerMapper institutionCustomerMapper;
    private final DeductionService deductionService;

    @Qualifier("collisionExecutor")
    private final Executor collisionExecutor;

    @Override
    public PreCheckResult competitivePreCheck(StandardApplyData data) {
        log.info("[APPLY] pre-check start, phoneMd5={}, channelCode={}, cityCode={}, age={}, amount={}",
                data.getPhoneMd5(), data.getChannelCode(), data.getCityCode(), data.getAge(), data.getLoanAmount());

        Channel channel = channelMapper.selectOne(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelCode, data.getChannelCode()));
        if (channel == null) {
            throw new RuntimeException("Channel not found: " + data.getChannelCode());
        }

        CollisionRecord record = createAndSaveCollisionRecord(data, channel);
        List<InstitutionProduct> matchedProducts = matchService.findMatchedProducts(data);
        log.info("[APPLY] matched products, collisionNo={}, products={}", record.getCollisionNo(), buildMatchedProductLog(matchedProducts));

        if (matchedProducts.isEmpty()) {
            updateCollisionSnapshot(record.getCollisionNo(), null, null, null, 9, "no matched product", null);
            return PreCheckResult.builder().pass(false).rejectReason("no matched product").build();
        }

        PreCheckRequest basePreCheckReq = createPreCheckRequest(data);
        List<CompletableFuture<PreCheckResult>> futures = matchedProducts.stream()
                .map(product -> CompletableFuture.supplyAsync(() -> preCheckSingleProduct(record, product, basePreCheckReq), collisionExecutor)
                        .completeOnTimeout((PreCheckResult) null, resolveInstitutionTimeoutMs(product), TimeUnit.MILLISECONDS))
                .collect(Collectors.toList());

        List<PreCheckResult> results = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .filter(PreCheckResult::isPass)
                .filter(r -> r.getPrice() != null)
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            updateCollisionSnapshot(record.getCollisionNo(), null, null, null, 9, "all institutions rejected", null);
            return PreCheckResult.builder().pass(false).rejectReason("all institutions rejected").build();
        }

        PreCheckResult winner = results.stream()
                .max(Comparator.comparing(PreCheckResult::getPrice))
                .orElse(null);

        if (winner != null) {
            InstitutionProduct winnerProduct = matchedProducts.stream()
                    .filter(product -> Objects.equals(product.getId(), winner.getProductId()))
                    .findFirst()
                    .orElse(null);
            updateCollisionSnapshot(record.getCollisionNo(), winner.getInstId(), winner.getProductId(),
                    winnerProduct == null ? null : winnerProduct.getProductName(), 0, null, winner.getPrice());
            log.info("[APPLY] winner selected, collisionNo={}, instCode={}, productId={}, price={}",
                    record.getCollisionNo(), winner.getInstCode(), winner.getProductId(), winner.getPrice());
            winner.setLocalOrderNo(record.getCollisionNo());
        }

        return winner;
    }

    @Override
    public CollisionRecord findLatestMatchedCollisionRecord(StandardApplyData data) {
        if (data == null || !StringUtils.hasText(data.getChannelCode()) || !StringUtils.hasText(data.getPhoneMd5())) {
            return null;
        }

        return collisionRecordMapper.selectOne(new LambdaQueryWrapper<CollisionRecord>()
                .eq(CollisionRecord::getChannelCode, data.getChannelCode())
                .eq(CollisionRecord::getPhoneMd5, data.getPhoneMd5())
                .eq(CollisionRecord::getCollisionStatus, 0)
                .isNotNull(CollisionRecord::getProductId)
                .isNotNull(CollisionRecord::getInstId)
                .orderByDesc(CollisionRecord::getCreatedAt)
                .last("LIMIT 1"));
    }

    @Override
    public PushResult pushToInstitution(StandardApplyData data, Long productId) {
        return pushToInstitution(data, productId, null);
    }

    @Override
    public PushResult pushToInstitution(StandardApplyData data, Long productId, String localOrderNo) {
        log.info("[PUSH] start push, productId={}, phoneMd5={}", productId, data.getPhoneMd5());

        InstitutionProduct product = institutionProductMapper.selectById(productId);
        if (product == null) {
            return PushResult.failure("Product not found: " + productId);
        }

        Institution inst = institutionMapper.selectById(product.getInstId());
        if (inst == null) {
            return PushResult.failure("Institution not found: " + product.getInstId());
        }

        InstitutionAdapter adapter = adapterRegistry.getAdapter(resolveAdapterKey(inst));
        if (adapter == null) {
            return PushResult.failure("Adapter not found: " + resolveAdapterKey(inst));
        }

        ApplyOrder order = getOrCreatePushOrder(data, localOrderNo, product, inst);
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String orderNo = order.getOrderNo();

        PushRequest pushReq = new PushRequest();
        pushReq.setOrderNo(orderNo);
        pushReq.setTraceId(traceId);
        pushReq.setProductId(productId);
        pushReq.setInstCode(inst.getInstCode());
        pushReq.setNotifyUrl(inst.getApiNotifyUrl());
        pushReq.setStandardData(data);

        log.info("[PUSH] dispatch productId={} productName={} instId={} instName={} instCode={} pushUrl={} notifyUrl={} orderNo={} traceId={}",
                product.getId(),
                product.getProductName(),
                inst.getId(),
                inst.getInstName(),
                inst.getInstCode(),
                inst.getApiPushUrl(),
                inst.getApiNotifyUrl(),
                orderNo,
                traceId);

        long start = System.currentTimeMillis();
        PushResult result = adapter.push(inst, pushReq);
        long cost = System.currentTimeMillis() - start;

        savePushExecution(order, traceId, product, inst, result, (int) cost);
        if (result != null && result.isSuccess()) {
            saveInstitutionCustomer(order, inst, product, result);
            deductionService.createPushSuccessDeduction(order.getOrderNo());
        }
        return result;
    }

    private CollisionRecord createAndSaveCollisionRecord(StandardApplyData data, Channel channel) {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        CollisionRecord record = new CollisionRecord();
        record.setCollisionNo(String.valueOf(System.currentTimeMillis()));
        record.setChannelId(channel.getId());
        record.setChannelCode(channel.getChannelCode());
        record.setTraceId(traceId);
        record.setPhoneMd5(data.getPhoneMd5());
        record.setPhoneEnc(AesUtil.encrypt(defaultString(data.getPhone()), channel.getAppKey()));
        record.setIdCardEnc(AesUtil.encrypt(defaultString(data.getIdCard()), channel.getAppKey()));
        record.setUserName(AesUtil.encrypt(defaultString(data.getName()), channel.getAppKey()));
        record.setUserNameMd5(StringUtils.hasText(data.getName()) ? DigestUtil.md5Hex(data.getName()) : null);
        record.setAge(data.getAge());
        record.setCityCode(data.getCityCode());
        record.setWorkCity(data.getWorkCity());
        record.setGender(data.getGender());
        record.setProfession(data.getProfession());
        record.setZhima(data.getZhima());
        record.setHouse(data.getHouse());
        record.setVehicle(data.getVehicle());
        record.setVehicleStatus(data.getVehicleStatus());
        record.setVehicleValue(data.getVehicleValue());
        record.setProvidentFund(data.getProvidentFund());
        record.setSocialSecurity(data.getSocialSecurity());
        record.setCommercialInsurance(data.getCommercialInsurance());
        record.setOverdue(data.getOverdue());
        record.setLoanAmount(data.getLoanAmount());
        record.setLoanTime(data.getLoanTime());
        record.setCustomerLevel(StringUtils.hasText(data.getCustomerLevel()) ? data.getCustomerLevel() : calculateCustomerLevel(data));
        record.setDeviceIp(data.getIp());
        record.setCollisionStatus(0);
        record.setExtJson(data.getExtraInfo() == null || data.getExtraInfo().isEmpty() ? null : JSON.toJSONString(data.getExtraInfo()));
        collisionRecordMapper.insert(record);
        return record;
    }

    private PreCheckResult preCheckSingleProduct(CollisionRecord record, InstitutionProduct product, PreCheckRequest basePreCheckReq) {
        try {
            Institution inst = institutionMapper.selectById(product.getInstId());
            if (inst == null) {
                return null;
            }

            InstitutionAdapter adapter = adapterRegistry.getAdapter(resolveAdapterKey(inst));
            if (adapter == null) {
                return null;
            }

            PreCheckRequest preCheckReq = copyPreCheckRequest(basePreCheckReq);
            preCheckReq.setProductId(product.getId());
            preCheckReq.setInstCode(inst.getInstCode());
            log.info("[APPLY] dispatch preCheck collisionNo={} productId={} productName={} instId={} instName={} instCode={} preCheckUrl={}",
                    record.getCollisionNo(),
                    product.getId(),
                    product.getProductName(),
                    inst.getId(),
                    inst.getInstName(),
                    inst.getInstCode(),
                    inst.getPreCheckUrl());
            long start = System.currentTimeMillis();
            PreCheckResult result = adapter.preCheck(inst, preCheckReq);
            long cost = System.currentTimeMillis() - start;

            if (result != null) {
                result.setProductId(product.getId());
                result.setInstId(inst.getId());
                result.setInstCode(inst.getInstCode());
                savePreCheckRecord(record, product, inst, result, (int) cost);
            }
            return result;
        } catch (Exception e) {
            log.error("[APPLY] pre-check failed, productId={}, collisionNo={}", product.getId(), record.getCollisionNo(), e);
            return null;
        }
    }

    private void savePreCheckRecord(CollisionRecord collisionRecord, InstitutionProduct product, Institution inst, PreCheckResult result, int cost) {
        PushRecord pushRecord = new PushRecord();
        pushRecord.setOrderNo(collisionRecord.getCollisionNo());
        pushRecord.setChannelId(collisionRecord.getChannelId());
        pushRecord.setInstId(inst.getId());
        pushRecord.setInstCode(inst.getInstCode());
        pushRecord.setProductId(product.getId());
        pushRecord.setTraceId(collisionRecord.getTraceId());
        pushRecord.setRequestId(result.getUuid());
        pushRecord.setPushStatus(result.isPass() ? 2 : 4);
        pushRecord.setResponseLog(JSON.toJSONString(result));
        pushRecord.setErrorMsg(result.getRejectReason());
        pushRecord.setCostMs(cost);
        pushRecord.setPushedAt(LocalDateTime.now());
        pushRecordMapper.insert(pushRecord);
    }

    private void savePushExecution(ApplyOrder order, String traceId, InstitutionProduct product, Institution inst, PushResult result, int cost) {
        PushRecord record = new PushRecord();
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setChannelId(order.getChannelId());
        record.setInstId(inst.getId());
        record.setInstCode(inst.getInstCode());
        record.setProductId(product.getId());
        record.setTraceId(traceId);
        record.setThirdOrderNo(result.getThirdOrderNo());
        record.setPushStatus(result.isSuccess() ? 2 : 4);
        record.setResponseLog(JSON.toJSONString(result));
        record.setErrorMsg(result.getErrorMsg());
        record.setCostMs(cost);
        record.setPushedAt(LocalDateTime.now());
        pushRecordMapper.insert(record);
        updateOrderAfterPush(order.getOrderNo(), record.getId(), inst.getId(), product.getId(), product.getProductName(), result);
    }

    private ApplyOrder getOrCreatePushOrder(StandardApplyData data, String localOrderNo,
                                            InstitutionProduct product, Institution inst) {
        ApplyOrder existingOrder = findOrderByCollisionNo(localOrderNo);
        if (existingOrder != null) {
            return existingOrder;
        }

        CollisionRecord collisionRecord = findCollisionRecordByNo(localOrderNo);
        Channel channel = channelMapper.selectOne(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelCode, data.getChannelCode())
                .last("LIMIT 1"));
        if (channel == null) {
            throw new RuntimeException("Channel not found: " + data.getChannelCode());
        }

        return createAndSaveApplyOrder(data, channel, collisionRecord, product, inst);
    }

    private ApplyOrder findOrderByCollisionNo(String collisionNo) {
        if (!StringUtils.hasText(collisionNo)) {
            return null;
        }
        return applyOrderMapper.selectOne(new LambdaQueryWrapper<ApplyOrder>()
                .like(ApplyOrder::getExtJson, "\"sourceCollisionNo\":\"" + collisionNo + "\"")
                .last("LIMIT 1"));
    }

    private CollisionRecord findCollisionRecordByNo(String collisionNo) {
        if (!StringUtils.hasText(collisionNo)) {
            return null;
        }
        return collisionRecordMapper.selectOne(new LambdaQueryWrapper<CollisionRecord>()
                .eq(CollisionRecord::getCollisionNo, collisionNo.trim())
                .last("LIMIT 1"));
    }

    private ApplyOrder findOrderByOrderNo(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }
        return applyOrderMapper.selectOne(new LambdaQueryWrapper<ApplyOrder>()
                .eq(ApplyOrder::getOrderNo, orderNo.trim())
                .last("LIMIT 1"));
    }

    private ApplyOrder createAndSaveApplyOrder(StandardApplyData data, Channel channel,
                                               CollisionRecord collisionRecord, InstitutionProduct product, Institution inst) {
        ApplyOrder order = new ApplyOrder();
        order.setOrderNo("A" + System.currentTimeMillis());
        order.setChannelId(channel.getId());
        order.setChannelCode(channel.getChannelCode());
        order.setInstId(inst.getId());
        order.setProductId(product.getId());
        order.setProductNameSnapshot(product.getProductName());
        order.setPhoneMd5(data.getPhoneMd5());
        order.setPhoneEnc(AesUtil.encrypt(defaultString(data.getPhone()), channel.getAppKey()));
        order.setIdCardEnc(AesUtil.encrypt(defaultString(data.getIdCard()), channel.getAppKey()));
        order.setUserName(AesUtil.encrypt(defaultString(data.getName()), channel.getAppKey()));
        order.setUserNameMd5(StringUtils.hasText(data.getName()) ? DigestUtil.md5Hex(data.getName()) : null);
        order.setAge(data.getAge());
        order.setCityCode(data.getCityCode());
        order.setWorkCity(data.getWorkCity());
        order.setGender(data.getGender());
        order.setProfession(data.getProfession());
        order.setZhima(data.getZhima());
        order.setHouse(data.getHouse());
        order.setVehicle(data.getVehicle());
        order.setVehicleStatus(data.getVehicleStatus());
        order.setVehicleValue(data.getVehicleValue());
        order.setProvidentFund(data.getProvidentFund());
        order.setSocialSecurity(data.getSocialSecurity());
        order.setCommercialInsurance(data.getCommercialInsurance());
        order.setOverdue(data.getOverdue());
        order.setLoanAmount(data.getLoanAmount());
        order.setLoanTime(data.getLoanTime());
        order.setCustomerLevel(StringUtils.hasText(data.getCustomerLevel()) ? data.getCustomerLevel() : calculateCustomerLevel(data));
        order.setDeviceIp(data.getIp());
        order.setOrderStatus(1);
        order.setSettlementPrice(collisionRecord == null ? null : collisionRecord.getSettlementPrice());
        order.setExtJson(buildApplyOrderExtJson(data, collisionRecord));
        applyOrderMapper.insert(order);
        return order;
    }

    private void updateOrderAfterPush(String orderNo, Long pushId, Long instId, Long productId,
                                      String productNameSnapshot, PushResult result) {
        LambdaUpdateWrapper<ApplyOrder> wrapper = new LambdaUpdateWrapper<ApplyOrder>()
                .eq(ApplyOrder::getOrderNo, orderNo)
                .set(ApplyOrder::getPushId, pushId)
                .set(ApplyOrder::getInstId, instId)
                .set(ApplyOrder::getProductId, productId)
                .set(ApplyOrder::getProductNameSnapshot, productNameSnapshot)
                .set(ApplyOrder::getOrderStatus, result != null && result.isSuccess() ? 1 : 9)
                .set(ApplyOrder::getRejectReason, result != null && result.isSuccess()
                        ? null
                        : result == null ? "push failed" : result.getErrorMsg());
        applyOrderMapper.update(null, wrapper);
    }

    private void saveInstitutionCustomer(ApplyOrder order, Institution inst, InstitutionProduct product, PushResult result) {
        if (order == null || inst == null) {
            return;
        }

        InstitutionCustomer existing = institutionCustomerMapper.selectOne(new LambdaQueryWrapper<InstitutionCustomer>()
                .eq(InstitutionCustomer::getOrderNo, order.getOrderNo())
                .last("LIMIT 1"));

        InstitutionCustomer customer = existing == null ? new InstitutionCustomer() : existing;
        customer.setOrderNo(order.getOrderNo());
        customer.setChannelId(order.getChannelId());
        customer.setChannelCode(order.getChannelCode());
        customer.setInstId(inst.getId());
        customer.setInstCode(inst.getInstCode());
        customer.setProductId(order.getProductId());
        customer.setProductNameSnapshot(StringUtils.hasText(order.getProductNameSnapshot())
                ? order.getProductNameSnapshot()
                : product == null ? null : product.getProductName());
        customer.setThirdOrderNo(result == null ? null : result.getThirdOrderNo());
        customer.setPhoneMd5(order.getPhoneMd5());
        customer.setPhoneEnc(order.getPhoneEnc());
        customer.setIdCardEnc(order.getIdCardEnc());
        customer.setUserName(order.getUserName());
        customer.setUserNameMd5(order.getUserNameMd5());
        customer.setAge(order.getAge());
        customer.setCityCode(order.getCityCode());
        customer.setWorkCity(order.getWorkCity());
        customer.setGender(order.getGender());
        customer.setProfession(order.getProfession());
        customer.setZhima(order.getZhima());
        customer.setHouse(order.getHouse());
        customer.setVehicle(order.getVehicle());
        customer.setVehicleStatus(order.getVehicleStatus());
        customer.setVehicleValue(order.getVehicleValue());
        customer.setProvidentFund(order.getProvidentFund());
        customer.setSocialSecurity(order.getSocialSecurity());
        customer.setCommercialInsurance(order.getCommercialInsurance());
        customer.setOverdue(order.getOverdue());
        customer.setLoanAmount(order.getLoanAmount());
        customer.setLoanTime(order.getLoanTime());
        customer.setCustomerLevel(order.getCustomerLevel());
        customer.setDeviceIp(order.getDeviceIp());
        customer.setSettlementPrice(order.getSettlementPrice());
        customer.setCustomerStatus(1);
        customer.setExtJson(order.getExtJson());

        if (existing == null) {
            institutionCustomerMapper.insert(customer);
        } else {
            institutionCustomerMapper.updateById(customer);
        }
    }

    private void updateCollisionSnapshot(String collisionNo, Long instId, Long productId, String productNameSnapshot,
                                         int status, String rejectReason, BigDecimal settlementPrice) {
        LambdaUpdateWrapper<CollisionRecord> wrapper = new LambdaUpdateWrapper<CollisionRecord>()
                .eq(CollisionRecord::getCollisionNo, collisionNo)
                .set(CollisionRecord::getCollisionStatus, status);

        if (instId != null) {
            wrapper.set(CollisionRecord::getInstId, instId);
        }
        if (productId != null) {
            wrapper.set(CollisionRecord::getProductId, productId);
        }
        if (productNameSnapshot != null) {
            wrapper.set(CollisionRecord::getProductNameSnapshot, productNameSnapshot);
        }
        if (rejectReason != null) {
            wrapper.set(CollisionRecord::getRejectReason, rejectReason);
        }
        if (settlementPrice != null) {
            wrapper.set(CollisionRecord::getSettlementPrice, settlementPrice);
        }

        collisionRecordMapper.update(null, wrapper);
    }

    private PreCheckRequest createPreCheckRequest(StandardApplyData data) {
        PreCheckRequest req = new PreCheckRequest();
        req.setPhoneMd5(data.getPhoneMd5());
        req.setPhone(data.getPhone());
        req.setIdCard(data.getIdCard());
        req.setName(data.getName());
        req.setAge(data.getAge());
        req.setCityCode(data.getCityCode());
        req.setWorkCity(data.getWorkCity());
        req.setAmount(data.getLoanAmount());
        req.setGender(data.getGender());
        req.setLoanTime(data.getLoanTime());
        req.setProfession(data.getProfession());
        req.setZhima(data.getZhima());
        req.setProvidentFund(data.getProvidentFund());
        req.setSocialSecurity(data.getSocialSecurity());
        req.setCommercialInsurance(data.getCommercialInsurance());
        req.setHouse(data.getHouse());
        req.setOverdue(data.getOverdue());
        req.setVehicle(data.getVehicle());
        return req;
    }

    private PreCheckRequest copyPreCheckRequest(PreCheckRequest source) {
        PreCheckRequest target = new PreCheckRequest();
        target.setPhone(source.getPhone());
        target.setIdCard(source.getIdCard());
        target.setName(source.getName());
        target.setPhoneMd5(source.getPhoneMd5());
        target.setIdCardMd5(source.getIdCardMd5());
        target.setAge(source.getAge());
        target.setCityCode(source.getCityCode());
        target.setWorkCity(source.getWorkCity());
        target.setAmount(source.getAmount());
        target.setGender(source.getGender());
        target.setLoanTime(source.getLoanTime());
        target.setProfession(source.getProfession());
        target.setZhima(source.getZhima());
        target.setProvidentFund(source.getProvidentFund());
        target.setSocialSecurity(source.getSocialSecurity());
        target.setCommercialInsurance(source.getCommercialInsurance());
        target.setHouse(source.getHouse());
        target.setOverdue(source.getOverdue());
        target.setVehicle(source.getVehicle());
        target.setProductId(source.getProductId());
        target.setInstCode(source.getInstCode());
        return target;
    }

    private String resolveAdapterKey(Institution institution) {
        if (institution == null) {
            return null;
        }
        return StringUtils.hasText(institution.getApiMethodName()) ? institution.getApiMethodName() : institution.getInstCode();
    }

    private String buildMatchedProductLog(List<InstitutionProduct> matchedProducts) {
        if (matchedProducts == null || matchedProducts.isEmpty()) {
            return "[]";
        }

        Map<Long, String> instCodeMap = institutionMapper.selectBatchIds(
                        matchedProducts.stream()
                                .map(InstitutionProduct::getInstId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(
                        Institution::getId,
                        Institution::getInstCode,
                        (left, right) -> left,
                        LinkedHashMap::new));

        return matchedProducts.stream()
                .map(product -> String.format("{productId=%s, productName=%s, instId=%s, instCode=%s, priority=%s}",
                        product.getId(),
                        product.getProductName(),
                        product.getInstId(),
                        instCodeMap.get(product.getInstId()),
                        product.getPriority()))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private long resolveInstitutionTimeoutMs(InstitutionProduct product) {
        if (product == null || product.getInstId() == null) {
            return 3000L;
        }
        Institution institution = institutionMapper.selectById(product.getInstId());
        if (institution == null || institution.getTimeoutMs() == null || institution.getTimeoutMs() <= 0) {
            return 3000L;
        }
        return institution.getTimeoutMs();
    }

    private String buildApplyOrderExtJson(StandardApplyData data, CollisionRecord collisionRecord) {
        Map<String, Object> extJson = new LinkedHashMap<>();
        if (data != null && data.getExtraInfo() != null && !data.getExtraInfo().isEmpty()) {
            extJson.putAll(data.getExtraInfo());
        }
        if (collisionRecord != null && StringUtils.hasText(collisionRecord.getCollisionNo())) {
            extJson.put("sourceCollisionNo", collisionRecord.getCollisionNo());
        }
        return extJson.isEmpty() ? null : JSON.toJSONString(extJson);
    }

    private String defaultString(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private String calculateCustomerLevel(StandardApplyData data) {
        int score = 0;
        if (Objects.equals(data.getHouse(), 1)) {
            score += 2;
        }
        if (Objects.equals(data.getVehicle(), 1)) {
            score += 2;
        }
        if (Objects.equals(data.getProvidentFund(), 1)) {
            score += 1;
        }
        if (Objects.equals(data.getSocialSecurity(), 1)) {
            score += 1;
        }
        if (Objects.equals(data.getCommercialInsurance(), 1)) {
            score += 1;
        }
        if (data.getZhima() != null) {
            if (data.getZhima() >= 700) {
                score += 2;
            } else if (data.getZhima() >= 650) {
                score += 1;
            }
        }
        if (Objects.equals(data.getOverdue(), 1)) {
            score += 1;
        }

        if (score == 0) {
            return null;
        }
        int stars = Math.max(1, Math.min(5, (score + 1) / 2));
        return stars + "星";
    }
}
