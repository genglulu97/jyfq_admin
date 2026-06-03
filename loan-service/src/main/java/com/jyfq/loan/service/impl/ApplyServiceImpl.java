package com.jyfq.loan.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.jyfq.loan.common.util.AesUtil;
import com.jyfq.loan.common.util.TraceUtil;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.mapper.CrmCustomerMapper;
import com.jyfq.loan.mapper.CrmInstitutionConfigMapper;
import com.jyfq.loan.mapper.CollisionPrecheckRecordMapper;
import com.jyfq.loan.mapper.CollisionRecordMapper;
import com.jyfq.loan.mapper.InstitutionCustomerMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.mapper.InstitutionProductMapper;
import com.jyfq.loan.mapper.PushRecordMapper;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.model.entity.ApplyOrder;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.entity.CollisionPrecheckRecord;
import com.jyfq.loan.model.entity.CollisionRecord;
import com.jyfq.loan.model.entity.CrmCustomer;
import com.jyfq.loan.model.entity.CrmInstitutionConfig;
import com.jyfq.loan.model.entity.InstitutionCustomer;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.entity.InstitutionProduct;
import com.jyfq.loan.model.entity.PushRecord;
import com.jyfq.loan.service.ApplyService;
import com.jyfq.loan.service.DeductionService;
import com.jyfq.loan.service.MatchService;
import com.jyfq.loan.thirdparty.InstitutionAdapter;
import com.jyfq.loan.thirdparty.InstitutionAdapterRegistry;
import com.jyfq.loan.thirdparty.MobileEightPreCheckAdapter;
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
import java.math.RoundingMode;
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

    private static final String PRICE_RETURN_MODE_AFTER_PROFIT = "AFTER_PROFIT";
    private static final String CHANNEL_PRICE_OUT_OF_RANGE = "channel price out of range";
    private static final String NO_MATCHED_PRODUCT = "no matched product";
    private static final String ALL_INSTITUTIONS_REJECTED = "all institutions rejected";
    private static final String CRM_CUSTOMER_EXISTS = "CRM customer already exists";
    private static final String CRM_CONFIG_NOT_FOUND = "CRM institution config not found";

    private final MatchService matchService;
    private final InstitutionAdapterRegistry adapterRegistry;
    private final InstitutionMapper institutionMapper;
    private final ApplyOrderMapper applyOrderMapper;
    private final CollisionPrecheckRecordMapper collisionPrecheckRecordMapper;
    private final CollisionRecordMapper collisionRecordMapper;
    private final ChannelMapper channelMapper;
    private final PushRecordMapper pushRecordMapper;
    private final InstitutionProductMapper institutionProductMapper;
    private final InstitutionCustomerMapper institutionCustomerMapper;
    private final CrmCustomerMapper crmCustomerMapper;
    private final CrmInstitutionConfigMapper crmInstitutionConfigMapper;
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
        log.info("【撞库流程】MD5:【{}】渠道：{}，撞库单：{}，匹配产品：{}",
                data.getPhoneMd5(), data.getChannelCode(), record.getCollisionNo(), buildMatchedProductLog(matchedProducts));

        if (matchedProducts.isEmpty()) {
            log.info("【撞库流程】MD5:【{}】渠道：{}，撞库单：{}，未匹配到产品",
                    data.getPhoneMd5(), data.getChannelCode(), record.getCollisionNo());
            saveCollisionPrecheckRecord(record, null, null, channel,
                    PreCheckResult.builder().pass(false).rejectReason(NO_MATCHED_PRODUCT).build(), 0, 4,
                    NO_MATCHED_PRODUCT);
            updateCollisionSnapshot(record.getCollisionNo(), null, null, null, 9, NO_MATCHED_PRODUCT, null);
            return PreCheckResult.builder()
                    .pass(false)
                    .rejectReason(NO_MATCHED_PRODUCT)
                    .products(List.of())
                    .build();
        }

        PreCheckRequest basePreCheckReq = createPreCheckRequest(data);
        log.info("【撞库流程】MD5:【{}】渠道：{}，撞库单：{}，匹配到{}个产品，开始并发撞库",
                data.getPhoneMd5(), data.getChannelCode(), record.getCollisionNo(), matchedProducts.size());
        List<CompletableFuture<PreCheckResult>> futures = matchedProducts.stream()
                .map(product -> CompletableFuture.supplyAsync(() -> preCheckSingleProduct(record, product, channel, basePreCheckReq), collisionExecutor)
                        .completeOnTimeout((PreCheckResult) null, resolveInstitutionTimeoutMs(product), TimeUnit.MILLISECONDS))
                .collect(Collectors.toList());

        List<PreCheckResult> allResults = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<PreCheckResult> results = allResults.stream()
                .filter(PreCheckResult::isPass)
                .filter(r -> r.getPrice() != null)
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            log.info("【撞库流程】MD5:【{}】渠道：{}，撞库单：{}，所有产品撞库均未通过",
                    data.getPhoneMd5(), data.getChannelCode(), record.getCollisionNo());
            String rejectReason = hasChannelPriceRejection(allResults) ? NO_MATCHED_PRODUCT : ALL_INSTITUTIONS_REJECTED;
            updateCollisionSnapshot(record.getCollisionNo(), null, null, null, 9, rejectReason, null);
            return PreCheckResult.builder()
                    .pass(false)
                    .rejectReason(rejectReason)
                    .products(List.of())
                    .build();
        }

        List<PreCheckResult> productResults = buildProductResultsForResponse(
                results, matchedProducts, record.getCollisionNo());
        PreCheckResult winner = productResults.isEmpty()
                ? null
                : copyProductResultForResponse(productResults.get(0), null, null, record.getCollisionNo());

        if (winner != null) {
            InstitutionProduct winnerProduct = matchedProducts.stream()
                    .filter(product -> Objects.equals(product.getId(), winner.getProductId()))
                    .findFirst()
                    .orElse(null);
            PriceSnapshot priceSnapshot = buildPriceSnapshot(winner, winnerProduct, channel);
            updateCollisionSnapshot(record.getCollisionNo(), winner.getInstId(), winner.getProductId(),
                    winnerProduct == null ? null : winnerProduct.getProductName(), 0, null,
                    priceSnapshot.productCoefficientPrice(), priceSnapshot);
            log.info("[APPLY] winner selected, collisionNo={}, instCode={}, productId={}, price={}",
                    record.getCollisionNo(), winner.getInstCode(), winner.getProductId(), winner.getPrice());
            log.info("【撞库流程】MD5:【{}】渠道：{}，撞库单：{}，最终命中产品：productId={}，instCode={}，price={}",
                    data.getPhoneMd5(), data.getChannelCode(), record.getCollisionNo(),
                    winner.getProductId(), winner.getInstCode(), winner.getPrice());
            winner.setLocalOrderNo(record.getCollisionNo());
            winner.setProducts(productResults);
        }

        return winner;
    }

    @Override
    public PreCheckResult mobileEightPreCheck(StandardApplyData data, String requestId, String mobileEight) {
        log.info("[APPLY] mobileEight pre-check start, phoneMd5={}, channelCode={}, cityCode={}, age={}, amount={}",
                data.getPhoneMd5(), data.getChannelCode(), data.getCityCode(), data.getAge(), data.getLoanAmount());
        Channel channel = channelMapper.selectOne(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelCode, data.getChannelCode())
                .eq(Channel::getStatus, 1)
                .last("LIMIT 1"));
        if (channel == null) {
            return PreCheckResult.builder().pass(false).rejectReason("channel not found or disabled").build();
        }

        CollisionRecord record = createAndSaveCollisionRecord(data, channel);
        List<InstitutionProduct> matchedProducts = matchService.findMatchedProducts(data);
        if (matchedProducts.isEmpty()) {
            saveCollisionPrecheckRecord(record, null, null, channel,
                    PreCheckResult.builder().pass(false).rejectReason(NO_MATCHED_PRODUCT).build(), 0, 4,
                    NO_MATCHED_PRODUCT);
            updateCollisionSnapshot(record.getCollisionNo(), null, null, null, 9, NO_MATCHED_PRODUCT, null);
            return PreCheckResult.builder()
                    .pass(false)
                    .rejectReason(NO_MATCHED_PRODUCT)
                    .products(List.of())
                    .build();
        }

        PreCheckRequest basePreCheckReq = createMobileEightPreCheckRequest(data, requestId, mobileEight, record.getCollisionNo());
        List<CompletableFuture<PreCheckResult>> futures = matchedProducts.stream()
                .map(product -> CompletableFuture.supplyAsync(
                        () -> preCheckSingleMobileEightProduct(record, product, channel, basePreCheckReq, data.getPhoneMd5()),
                        collisionExecutor)
                        .completeOnTimeout((PreCheckResult) null, resolveInstitutionTimeoutMs(product), TimeUnit.MILLISECONDS))
                .collect(Collectors.toList());

        List<PreCheckResult> allResults = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<PreCheckResult> results = allResults.stream()
                .filter(PreCheckResult::isPass)
                .filter(r -> r.getPrice() != null)
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            String rejectReason = hasChannelPriceRejection(allResults) ? NO_MATCHED_PRODUCT : ALL_INSTITUTIONS_REJECTED;
            updateCollisionSnapshot(record.getCollisionNo(), null, null, null, 9, rejectReason, null);
            return PreCheckResult.builder()
                    .pass(false)
                    .rejectReason(rejectReason)
                    .products(List.of())
                    .build();
        }

        List<PreCheckResult> productResults = buildProductResultsForResponse(results, matchedProducts, record.getCollisionNo());
        PreCheckResult winner = productResults.isEmpty()
                ? null
                : copyProductResultForResponse(productResults.get(0), null, null, record.getCollisionNo());
        if (winner != null) {
            InstitutionProduct winnerProduct = matchedProducts.stream()
                    .filter(product -> Objects.equals(product.getId(), winner.getProductId()))
                    .findFirst()
                    .orElse(null);
            PriceSnapshot priceSnapshot = buildPriceSnapshot(winner, winnerProduct, channel);
            updateCollisionSnapshot(record.getCollisionNo(), winner.getInstId(), winner.getProductId(),
                    winnerProduct == null ? null : winnerProduct.getProductName(), 0, null,
                    priceSnapshot.productCoefficientPrice(), priceSnapshot);
            winner.setLocalOrderNo(record.getCollisionNo());
            winner.setProducts(productResults);
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
    public CollisionRecord findMatchedCollisionRecord(StandardApplyData data, String collisionNo) {
        if (data == null
                || !StringUtils.hasText(collisionNo)
                || !StringUtils.hasText(data.getChannelCode())
                || !StringUtils.hasText(data.getPhoneMd5())) {
            return null;
        }

        return collisionRecordMapper.selectOne(new LambdaQueryWrapper<CollisionRecord>()
                .eq(CollisionRecord::getCollisionNo, collisionNo.trim())
                .eq(CollisionRecord::getChannelCode, data.getChannelCode())
                .eq(CollisionRecord::getPhoneMd5, data.getPhoneMd5())
                .eq(CollisionRecord::getCollisionStatus, 0)
                .isNotNull(CollisionRecord::getProductId)
                .isNotNull(CollisionRecord::getInstId)
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

        CrmInstitutionConfig crmConfig = findActiveCrmConfig(inst.getId());
        if (crmConfig != null) {
            log.info("[CRM] 进入CRM入池, productId={}, productName={}, instId={}, instCode={}, configId={}, collisionNo={}",
                    product.getId(), product.getProductName(), inst.getId(), inst.getInstCode(), crmConfig.getId(), localOrderNo);
            return pushToCrm(data, product, inst, crmConfig, localOrderNo);
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
        PushResult result;
        try {
            result = adapter.push(inst, pushReq);
        } catch (Exception ex) {
            log.error("[PUSH] dispatch exception, instCode={}, orderNo={}", inst.getInstCode(), orderNo, ex);
            result = PushResult.failure("push exception: " + ex.getMessage());
            result.setInstCode(inst.getInstCode());
        }
        long cost = System.currentTimeMillis() - start;

        savePushExecution(order, traceId, product, inst, result, (int) cost);
        if (result != null && result.isSuccess()) {
            saveInstitutionCustomer(order, inst, product, result);
            deductionService.createPushSuccessDeduction(order.getOrderNo());
        }
        return result;
    }

    private PushResult pushToCrm(StandardApplyData data, InstitutionProduct product, Institution inst,
                                 CrmInstitutionConfig config, String localOrderNo) {
        long start = System.currentTimeMillis();
        if (config == null) {
            PushResult result = PushResult.failure(CRM_CONFIG_NOT_FOUND);
            result.setInstCode(inst.getInstCode());
            return result;
        }
        if (crmCustomerExists(inst.getId(), data.getPhone(), data.getPhoneMd5())) {
            PushResult result = PushResult.failure(CRM_CUSTOMER_EXISTS);
            result.setInstCode(inst.getInstCode());
            return result;
        }

        ApplyOrder order = getOrCreatePushOrder(data, localOrderNo, product, inst);
        String traceId = UUID.randomUUID().toString().replace("-", "");
        CrmCustomer customer = createCrmCustomerFromApply(data, order, product, inst, config, localOrderNo);

        PushResult result = PushResult.success("CRM internal push success");
        result.setInstCode(inst.getInstCode());
        result.setThirdOrderNo(String.valueOf(customer.getId()));
        result.setCostMs(System.currentTimeMillis() - start);

        savePushExecution(order, traceId, product, inst, result, (int) result.getCostMs());
        saveInstitutionCustomer(order, inst, product, result);
        deductionService.createPushSuccessDeduction(order.getOrderNo());
        log.info("[PUSH] CRM internal push completed, orderNo={}, crmCustomerId={}, instCode={}",
                order.getOrderNo(), customer.getId(), inst.getInstCode());
        return result;
    }

    private CollisionRecord createAndSaveCollisionRecord(StandardApplyData data, Channel channel) {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        CollisionRecord record = new CollisionRecord();
        record.setCollisionNo(IdWorker.getIdStr());
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
        record.setCustomerLevel(null);
        record.setDeviceIp(data.getIp());
        record.setCollisionStatus(0);
        record.setExtJson(data.getExtraInfo() == null || data.getExtraInfo().isEmpty() ? null : JSON.toJSONString(data.getExtraInfo()));
        collisionRecordMapper.insert(record);
        return record;
    }

    private PreCheckResult preCheckSingleProduct(CollisionRecord record, InstitutionProduct product,
                                                 Channel channel, PreCheckRequest basePreCheckReq) {
        Institution inst = null;
        long start = System.currentTimeMillis();
        try {
            inst = institutionMapper.selectById(product.getInstId());
            if (inst == null) {
                log.warn("【{} 掩码撞库】MD5:【{}】，产品：{}，机构不存在，instId={}",
                        product.getProductName(), record.getPhoneMd5(), product.getId(), product.getInstId());
                saveCollisionPrecheckRecord(record, product, null, channel,
                        PreCheckResult.builder().pass(false).rejectReason("institution not found").build(), 0, 9,
                        "institution not found");
                return null;
            }

            CrmInstitutionConfig crmConfig = findActiveCrmConfig(inst.getId());
            if (crmConfig != null) {
                log.info("[CRM] 进入CRM匹配, collisionNo={}, productId={}, productName={}, instId={}, instCode={}, configId={}",
                        record.getCollisionNo(), product.getId(), product.getProductName(), inst.getId(), inst.getInstCode(), crmConfig.getId());
                return preCheckCrmProduct(record, product, inst, channel, basePreCheckReq, start, crmConfig);
            }

            InstitutionAdapter adapter = adapterRegistry.getAdapter(resolveAdapterKey(inst));
            if (adapter == null) {
                log.warn("【{} 掩码撞库】MD5:【{}】，产品：{}，机构：{}，适配器不存在：{}",
                        product.getProductName(), record.getPhoneMd5(), product.getId(), inst.getId(), resolveAdapterKey(inst));
                saveCollisionPrecheckRecord(record, product, inst, channel,
                        PreCheckResult.builder().pass(false).rejectReason("adapter not found").build(), 0, 9,
                        "adapter not found");
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
            log.info("【{} 掩码撞库】MD5:【{}】，产品：{}，机构：{}，撞库开始，请求地址：{}",
                    product.getProductName(),
                    record.getPhoneMd5(),
                    product.getId(),
                    inst.getId(),
                    inst.getPreCheckUrl());
            start = System.currentTimeMillis();
            if (!(adapter instanceof MobileEightPreCheckAdapter mobileEightAdapter)) {
                saveCollisionPrecheckRecord(record, product, inst, channel,
                        PreCheckResult.builder().pass(false).rejectReason("mobileEight preCheck not supported").build(), 0, 9,
                        "mobileEight preCheck not supported");
                return null;
            }

            PreCheckResult result = mobileEightAdapter.mobileEightPreCheck(inst, preCheckReq);
            long cost = System.currentTimeMillis() - start;

            if (result != null) {
                result.setProductId(product.getId());
                result.setInstId(inst.getId());
                result.setInstCode(inst.getInstCode());
                PriceSnapshot priceSnapshot = buildPriceSnapshot(result, product, channel);
                fillPreCheckPriceFields(result, priceSnapshot);
                applyChannelPricePolicy(result, priceSnapshot, channel);
                saveCollisionPrecheckRecord(record, product, inst, channel, result, (int) cost,
                        result.isPass() ? 2 : 4, result.getRejectReason());
                if (result.isPass()) {
                    log.info("【{} 掩码撞库】MD5:【{}】，产品：{}，机构：{}，耗时：{}ms，撞库通过，返回值：{}",
                            product.getProductName(), record.getPhoneMd5(), product.getId(), inst.getId(), cost, JSON.toJSONString(result));
                } else {
                    log.warn("【{} 掩码撞库】MD5:【{}】，产品：{}，机构：{}，耗时：{}ms，撞库失败，返回值：{}",
                            product.getProductName(), record.getPhoneMd5(), product.getId(), inst.getId(), cost, JSON.toJSONString(result));
                }
            } else {
                log.warn("【{} 掩码撞库】MD5:【{}】，产品：{}，机构：{}，耗时：{}ms，撞库返回为空",
                        product.getProductName(), record.getPhoneMd5(), product.getId(), inst.getId(), cost);
                saveCollisionPrecheckRecord(record, product, inst, channel,
                        PreCheckResult.builder().pass(false).rejectReason("preCheck result is null").build(), (int) cost, 9,
                        "preCheck result is null");
            }
            return result;
        } catch (Exception e) {
            log.error("[APPLY] pre-check failed, productId={}, collisionNo={}", product.getId(), record.getCollisionNo(), e);
            saveCollisionPrecheckRecord(record, product, inst, channel,
                    PreCheckResult.builder().pass(false).rejectReason("preCheck exception: " + e.getMessage()).build(),
                    (int) (System.currentTimeMillis() - start), 9, "preCheck exception: " + e.getMessage());
            return null;
        }
    }

    private PreCheckResult preCheckSingleMobileEightProduct(CollisionRecord record, InstitutionProduct product,
                                                            Channel channel, PreCheckRequest basePreCheckReq,
                                                            String currentPhoneMd5) {
        Institution inst = null;
        long start = System.currentTimeMillis();
        try {
            inst = institutionMapper.selectById(product.getInstId());
            if (inst == null) {
                saveCollisionPrecheckRecord(record, product, null, channel,
                        PreCheckResult.builder().pass(false).rejectReason("institution not found").build(), 0, 9,
                        "institution not found");
                return null;
            }

            InstitutionAdapter adapter = adapterRegistry.getAdapter(resolveAdapterKey(inst));
            if (adapter == null) {
                saveCollisionPrecheckRecord(record, product, inst, channel,
                        PreCheckResult.builder().pass(false).rejectReason("adapter not found").build(), 0, 9,
                        "adapter not found");
                return null;
            }

            PreCheckRequest preCheckReq = copyPreCheckRequest(basePreCheckReq);
            preCheckReq.setProductId(product.getId());
            preCheckReq.setInstCode(inst.getInstCode());

            PreCheckResult result = adapter.preCheck(inst, preCheckReq);
            long cost = System.currentTimeMillis() - start;
            if (result == null) {
                saveCollisionPrecheckRecord(record, product, inst, channel,
                        PreCheckResult.builder().pass(false).rejectReason("preCheck result is null").build(), (int) cost, 9,
                        "preCheck result is null");
                return null;
            }

            result.setInstCode(inst.getInstCode());
            result.setInstId(inst.getId());
            result.setProductId(product.getId());
            boolean hitCurrentPhone = containsCurrentPhoneMd5(result, currentPhoneMd5);
            if (hitCurrentPhone) {
                result.setPass(false);
                result.setRejectReason("mobile md5 hit downstream list");
            }
            if (result.isPass()) {
                fillPreCheckPriceFields(result, buildPriceSnapshot(result, product, channel));
            }
            saveCollisionPrecheckRecord(record, product, inst, channel, result, (int) cost,
                    result.isPass() ? 0 : 9, result.getRejectReason());
            return result;
        } catch (Exception e) {
            saveCollisionPrecheckRecord(record, product, inst, channel,
                    PreCheckResult.builder().pass(false).rejectReason("preCheck exception: " + e.getMessage()).build(),
                    (int) (System.currentTimeMillis() - start), 9, "preCheck exception: " + e.getMessage());
            return null;
        }
    }

    private boolean containsCurrentPhoneMd5(PreCheckResult result, String currentPhoneMd5) {
        if (result == null || !StringUtils.hasText(currentPhoneMd5)
                || result.getMobileList() == null || result.getMobileList().isEmpty()) {
            return false;
        }
        String normalized = currentPhoneMd5.trim().toLowerCase();
        return result.getMobileList().stream()
                .filter(StringUtils::hasText)
                .map(item -> item.trim().toLowerCase())
                .anyMatch(normalized::equals);
    }

    private PreCheckResult preCheckCrmProduct(CollisionRecord record, InstitutionProduct product, Institution inst,
                                              Channel channel, PreCheckRequest request, long start,
                                              CrmInstitutionConfig config) {
        PreCheckResult result;
        if (config == null) {
            result = PreCheckResult.builder()
                    .pass(false)
                    .rejectReason(CRM_CONFIG_NOT_FOUND)
                    .requestLog(JSON.toJSONString(request))
                    .build();
            saveCollisionPrecheckRecord(record, product, inst, channel, result,
                    (int) (System.currentTimeMillis() - start), 4, CRM_CONFIG_NOT_FOUND);
            return result;
        }
        if (crmCustomerExists(inst.getId(), request.getPhone(), record.getPhoneMd5())) {
            result = PreCheckResult.builder()
                    .pass(false)
                    .rejectReason(CRM_CUSTOMER_EXISTS)
                    .requestLog(JSON.toJSONString(request))
                    .build();
            saveCollisionPrecheckRecord(record, product, inst, channel, result,
                    (int) (System.currentTimeMillis() - start), 4, CRM_CUSTOMER_EXISTS);
            return result;
        }

        result = PreCheckResult.builder()
                .pass(true)
                .price(defaultDecimal(product.getUnitPrice(), BigDecimal.ZERO))
                .downstreamPrice(defaultDecimal(product.getUnitPrice(), BigDecimal.ZERO))
                .uuid(record.getCollisionNo())
                .orderId(record.getCollisionNo())
                .productLogo(product.getProductIcon())
                .productName(product.getProductName())
                .companyName(resolveInstitutionName(inst))
                .protocolList(resolveProtocolList(null, product))
                .productId(product.getId())
                .instId(inst.getId())
                .instCode(inst.getInstCode())
                .instName(resolveInstitutionName(inst))
                .localOrderNo(record.getCollisionNo())
                .requestLog(JSON.toJSONString(request))
                .responseLog("CRM_INTERNAL_PASS")
                .build();
        PriceSnapshot priceSnapshot = buildPriceSnapshot(result, product, channel);
        fillPreCheckPriceFields(result, priceSnapshot);
        applyChannelPricePolicy(result, priceSnapshot, channel);
        saveCollisionPrecheckRecord(record, product, inst, channel, result,
                (int) (System.currentTimeMillis() - start), result.isPass() ? 2 : 4, result.getRejectReason());
        log.info("[APPLY] CRM internal preCheck completed, collisionNo={}, instCode={}, productId={}, pass={}",
                record.getCollisionNo(), inst.getInstCode(), product.getId(), result.isPass());
        return result;
    }

    private void saveCollisionPrecheckRecord(CollisionRecord collisionRecord, InstitutionProduct product, Institution inst,
                                             Channel channel, PreCheckResult result, int cost,
                                             int status, String fallbackErrorMsg) {
        if (collisionRecord == null) {
            return;
        }
        PriceSnapshot priceSnapshot = buildPriceSnapshot(result, product, channel);
        fillPreCheckPriceFields(result, priceSnapshot);

        CollisionPrecheckRecord detail = new CollisionPrecheckRecord();
        detail.setCollisionId(collisionRecord.getId());
        detail.setCollisionNo(collisionRecord.getCollisionNo());
        detail.setChannelId(collisionRecord.getChannelId());
        detail.setChannelCode(collisionRecord.getChannelCode());
        detail.setInstId(inst == null ? (product == null ? null : product.getInstId()) : inst.getId());
        detail.setInstCode(inst == null ? null : inst.getInstCode());
        detail.setProductId(product == null ? null : product.getId());
        detail.setProductNameSnapshot(product == null ? null : product.getProductName());
        detail.setTraceId(collisionRecord.getTraceId());
        detail.setRequestId(result == null ? null : result.getUuid());
        detail.setThirdOrderNo(result == null ? null : result.getOrderId());
        detail.setPrecheckStatus(status);
        detail.setRequestLog(result == null ? null : result.getRequestLog());
        detail.setResponseLog(resolvePrecheckResponseLog(result));
        detail.setDownstreamPrice(priceSnapshot.downstreamPrice());
        detail.setProductCoefficientPrice(priceSnapshot.productCoefficientPrice());
        detail.setUpstreamChannelPrice(priceSnapshot.upstreamChannelPrice());
        detail.setErrorMsg(limitErrorMsg(resolvePrecheckErrorMsg(result, fallbackErrorMsg)));
        detail.setCostMs(cost);
        detail.setPrecheckedAt(LocalDateTime.now());
        collisionPrecheckRecordMapper.insert(detail);
    }

    private String resolvePrecheckResponseLog(PreCheckResult result) {
        if (result == null) {
            return null;
        }
        if (StringUtils.hasText(result.getResponseLog())) {
            return result.getResponseLog();
        }
        return JSON.toJSONString(result);
    }

    private String resolvePrecheckErrorMsg(PreCheckResult result, String fallbackErrorMsg) {
        if (result != null && StringUtils.hasText(result.getRejectReason())) {
            return result.getRejectReason();
        }
        return fallbackErrorMsg;
    }

    private String limitErrorMsg(String errorMsg) {
        if (!StringUtils.hasText(errorMsg)) {
            return null;
        }
        return errorMsg.length() > 512 ? errorMsg.substring(0, 512) : errorMsg;
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
        record.setThirdOrderNo(result == null ? null : result.getThirdOrderNo());
        record.setPushStatus(result != null && result.isSuccess() ? 2 : 4);
        record.setResponseLog(JSON.toJSONString(result));
        record.setErrorMsg(result == null ? "push result is null" : result.getErrorMsg());
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
        order.setOrderNo("A" + IdWorker.getIdStr());
        order.setChannelId(channel.getId());
        order.setChannelCode(channel.getChannelCode());
        order.setInstId(inst.getId());
        order.setProductId(product.getId());
        order.setProductNameSnapshot(product.getProductName());
        order.setTraceId(resolveApplyOrderTraceId(collisionRecord));
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
        order.setCustomerLevel(null);
        order.setDeviceIp(data.getIp());
        order.setOrderStatus(0);
        order.setSettlementPrice(collisionRecord == null ? null : collisionRecord.getSettlementPrice());
        order.setExtJson(buildApplyOrderExtJson(data, collisionRecord));
        applyOrderMapper.insert(order);
        return order;
    }

    private String resolveApplyOrderTraceId(CollisionRecord collisionRecord) {
        if (collisionRecord != null && StringUtils.hasText(collisionRecord.getTraceId())) {
            return collisionRecord.getTraceId();
        }
        String currentTraceId = TraceUtil.get();
        if (StringUtils.hasText(currentTraceId)) {
            return currentTraceId;
        }
        return UUID.randomUUID().toString().replace("-", "");
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

    private CrmCustomer createCrmCustomerFromApply(StandardApplyData data, ApplyOrder order, InstitutionProduct product,
                                                   Institution inst, CrmInstitutionConfig config, String sourceCollisionNo) {
        CrmCustomer customer = new CrmCustomer();
        customer.setCustomerName(defaultString(data.getName()));
        customer.setMobile(resolveCrmMobile(data));
        customer.setMobileMd5(resolvePhoneMd5(data.getPhone(), data.getPhoneMd5()));
        customer.setCrmInstId(inst.getId());
        customer.setCrmInstCode(inst.getInstCode());
        customer.setCrmInstName(inst.getInstName());
        customer.setSourceInstId(config.getPlatformInstId());
        customer.setSourceInstCode(config.getPlatformInstCode());
        customer.setSourceInstName(config.getPlatformInstName());
        customer.setProductId(product.getId());
        customer.setProductName(product.getProductName());
        customer.setSourceOrderNo(order.getOrderNo());
        customer.setSourceCollisionNo(sourceCollisionNo);
        customer.setIdCard(data.getIdCard());
        customer.setCity(firstText(data.getWorkCity(), data.getCityCode()));
        customer.setAge(data.getAge());
        customer.setGender(resolveGender(data.getGender()));
        customer.setLoanAmount(data.getLoanAmount() == null ? null : BigDecimal.valueOf(data.getLoanAmount()));
        customer.setLoanPurpose(extraText(data, "loanPurpose"));
        customer.setExpectedTerm(data.getLoanTime() == null ? null : String.valueOf(data.getLoanTime()));
        customer.setCustomerSource(firstText(config.getCustomerSource(), "CRM_API"));
        customer.setChannelCode(data.getChannelCode());
        customer.setOwnerAdminId(Integer.valueOf(1).equals(config.getAutoAssign()) ? config.getOwnerAdminId() : null);
        customer.setOwnerName(Integer.valueOf(1).equals(config.getAutoAssign()) ? config.getOwnerName() : null);
        customer.setTeamId(config.getTeamId());
        customer.setCustomerStatus("UNFOLLOWED");
        customer.setLoanIntention("UNCONFIRMED");
        customer.setQualityStar(3);
        customer.setFollowCount(0);
        customer.setIsAllocated(customer.getOwnerAdminId() == null ? 0 : 1);
        customer.setIsCalled(0);
        customer.setIsDuplicate(0);
        customer.setIsValid(1);
        customer.setWechatAdded(0);
        customer.setNeedRecall(0);
        customer.setIsDeal(0);
        customer.setIsRejected(0);
        customer.setIsKeyCustomer(0);
        customer.setInPublicPool(customer.getOwnerAdminId() == null ? 1 : 0);
        customer.setPublicPoolReason(customer.getOwnerAdminId() == null ? "CRM_API_PUSH" : null);
        customer.setHasHouse(toYesNo(data.getHouse()));
        customer.setHasCar(toYesNo(data.getVehicle()));
        customer.setHasSocialSecurity(toYesNo(data.getSocialSecurity()));
        customer.setHasHousingFund(toYesNo(data.getProvidentFund()));
        customer.setSesameScore(data.getZhima());
        customer.setHasOverdue(toYesNo(data.getOverdue()));
        customer.setRemark("CRM internal push, orderNo=" + order.getOrderNo()
                + ", instCode=" + inst.getInstCode()
                + ", productId=" + product.getId());
        customer.setCreatedAt(LocalDateTime.now());
        crmCustomerMapper.insert(customer);
        return customer;
    }

    private boolean crmCustomerExists(Long crmInstId, String phone, String phoneMd5) {
        LambdaQueryWrapper<CrmCustomer> wrapper = new LambdaQueryWrapper<>();
        if (crmInstId != null) {
            wrapper.eq(CrmCustomer::getCrmInstId, crmInstId);
        }
        if (StringUtils.hasText(phone)) {
            wrapper.and(item -> {
                item.eq(CrmCustomer::getMobile, phone.trim());
                if (StringUtils.hasText(phoneMd5)) {
                    item.or().eq(CrmCustomer::getMobileMd5, phoneMd5.trim());
                }
            });
        } else if (StringUtils.hasText(phoneMd5)) {
            wrapper.eq(CrmCustomer::getMobileMd5, phoneMd5.trim());
        } else {
            return false;
        }
        return crmCustomerMapper.selectCount(wrapper) > 0;
    }

    private CrmInstitutionConfig findActiveCrmConfig(Long instId) {
        if (instId == null) {
            return null;
        }
        return crmInstitutionConfigMapper.selectOne(new LambdaQueryWrapper<CrmInstitutionConfig>()
                .and(wrapper -> wrapper.eq(CrmInstitutionConfig::getInstId, instId)
                        .or()
                        .eq(CrmInstitutionConfig::getCrmInstId, instId))
                .eq(CrmInstitutionConfig::getStatus, 1)
                .last("LIMIT 1"));
    }

    private String resolveCrmMobile(StandardApplyData data) {
        if (data != null && StringUtils.hasText(data.getPhone())) {
            return data.getPhone().trim();
        }
        if (data != null && StringUtils.hasText(data.getPhoneMd5())) {
            return data.getPhoneMd5().trim();
        }
        return "";
    }

    private String resolvePhoneMd5(String phone, String phoneMd5) {
        if (StringUtils.hasText(phoneMd5)) {
            return phoneMd5.trim();
        }
        return StringUtils.hasText(phone) ? DigestUtil.md5Hex(phone.trim()) : null;
    }

    private String resolveGender(Integer gender) {
        if (gender == null) {
            return null;
        }
        if (gender == 1) {
            return "MALE";
        }
        if (gender == 2) {
            return "FEMALE";
        }
        return "UNKNOWN";
    }

    private Integer toYesNo(Integer value) {
        if (value == null) {
            return null;
        }
        return value > 0 && value != 2 ? 1 : 0;
    }

    private String extraText(StandardApplyData data, String key) {
        if (data == null || data.getExtraInfo() == null || !data.getExtraInfo().containsKey(key)) {
            return null;
        }
        Object value = data.getExtraInfo().get(key);
        return value == null ? null : value.toString();
    }

    private void updateCollisionSnapshot(String collisionNo, Long instId, Long productId, String productNameSnapshot,
                                         int status, String rejectReason, BigDecimal settlementPrice) {
        updateCollisionSnapshot(collisionNo, instId, productId, productNameSnapshot, status, rejectReason, settlementPrice, null);
    }

    private void updateCollisionSnapshot(String collisionNo, Long instId, Long productId, String productNameSnapshot,
                                         int status, String rejectReason, BigDecimal settlementPrice, PriceSnapshot priceSnapshot) {
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
        if (priceSnapshot != null) {
            if (priceSnapshot.downstreamPrice() != null) {
                wrapper.set(CollisionRecord::getDownstreamPrice, priceSnapshot.downstreamPrice());
            }
            if (priceSnapshot.productCoefficientPrice() != null) {
                wrapper.set(CollisionRecord::getProductCoefficientPrice, priceSnapshot.productCoefficientPrice());
            }
            if (priceSnapshot.upstreamChannelPrice() != null) {
                wrapper.set(CollisionRecord::getUpstreamChannelPrice, priceSnapshot.upstreamChannelPrice());
            }
        }

        collisionRecordMapper.update(null, wrapper);
    }

    private List<PreCheckResult> buildProductResultsForResponse(List<PreCheckResult> results,
                                                                 List<InstitutionProduct> matchedProducts,
                                                                 String localOrderNo) {
        Map<Long, InstitutionProduct> productMap = matchedProducts.stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(
                        InstitutionProduct::getId,
                        product -> product,
                        (left, right) -> left,
                        LinkedHashMap::new));
        Map<Long, Institution> institutionMap = buildInstitutionMap(results, productMap);

        return results.stream()
                .sorted(Comparator.comparing(PreCheckResult::getPrice).reversed())
                .map(result -> {
                    InstitutionProduct product = productMap.get(result.getProductId());
                    Institution institution = institutionMap.get(resolveInstId(result, product));
                    return copyProductResultForResponse(result, product, institution, localOrderNo);
                })
                .collect(Collectors.toList());
    }

    private Map<Long, Institution> buildInstitutionMap(List<PreCheckResult> results,
                                                       Map<Long, InstitutionProduct> productMap) {
        List<Long> instIds = results.stream()
                .map(result -> resolveInstId(result, productMap.get(result.getProductId())))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (instIds.isEmpty()) {
            return Map.of();
        }

        return institutionMapper.selectBatchIds(instIds).stream()
                .collect(Collectors.toMap(
                        Institution::getId,
                        institution -> institution,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private PreCheckResult copyProductResultForResponse(PreCheckResult source,
                                                        InstitutionProduct product,
                                                        Institution institution,
                                                        String localOrderNo) {
        Long instId = resolveInstId(source, product);
        return PreCheckResult.builder()
                .pass(source.isPass())
                .price(source.getPrice())
                .downstreamPrice(source.getDownstreamPrice())
                .productCoefficientPrice(source.getProductCoefficientPrice())
                .upstreamChannelPrice(source.getUpstreamChannelPrice())
                .uuid(source.getUuid())
                .orderId(source.getOrderId())
                .productLogo(firstText(source.getProductLogo(), product == null ? null : product.getProductIcon()))
                .productName(firstText(product == null ? null : product.getProductName(), source.getProductName()))
                .companyName(firstText(source.getCompanyName(), resolveInstitutionName(institution)))
                .protocolList(resolveProtocolList(source.getProtocolList(), product))
                .productId(source.getProductId())
                .instId(instId)
                .instCode(firstText(source.getInstCode(), institution == null ? null : institution.getInstCode()))
                .instName(firstText(resolveInstitutionName(institution), source.getInstName()))
                .localOrderNo(localOrderNo)
                .rejectReason(source.getRejectReason())
                .build();
    }

    private Long resolveInstId(PreCheckResult result, InstitutionProduct product) {
        if (result != null && result.getInstId() != null) {
            return result.getInstId();
        }
        return product == null ? null : product.getInstId();
    }

    private String resolveInstitutionName(Institution institution) {
        if (institution == null) {
            return null;
        }
        return firstText(institution.getInstName(), institution.getInstCode());
    }

    private List<Map<String, Object>> resolveProtocolList(List<Map<String, Object>> source,
                                                          InstitutionProduct product) {
        if (source != null && !source.isEmpty()) {
            return source;
        }
        if (product != null && StringUtils.hasText(product.getProtocolUrl())) {
            Map<String, Object> protocol = new LinkedHashMap<>();
            protocol.put("name", "服务协议");
            protocol.put("url", product.getProtocolUrl());
            return List.of(protocol);
        }
        return source;
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private PriceSnapshot buildPriceSnapshot(PreCheckResult result, InstitutionProduct product, Channel channel) {
        BigDecimal downstreamPrice = normalizePrice(resolveDownstreamPrice(result));
        BigDecimal productCoefficientPrice = multiplyPrice(
                downstreamPrice,
                product == null ? BigDecimal.ONE : defaultDecimal(product.getPriceRatio(), BigDecimal.ONE));
        BigDecimal upstreamChannelPrice = multiplyPrice(
                productCoefficientPrice,
                channel == null ? BigDecimal.ZERO : defaultDecimal(channel.getFeeRate(), BigDecimal.ZERO));
        BigDecimal responsePrice = isAfterProfitPriceMode(channel) ? upstreamChannelPrice : downstreamPrice;
        return new PriceSnapshot(downstreamPrice, productCoefficientPrice, upstreamChannelPrice, responsePrice);
    }

    private void fillPreCheckPriceFields(PreCheckResult result, PriceSnapshot priceSnapshot) {
        if (result == null || priceSnapshot == null) {
            return;
        }
        result.setPrice(priceSnapshot.responsePrice());
        result.setDownstreamPrice(priceSnapshot.downstreamPrice());
        result.setProductCoefficientPrice(priceSnapshot.productCoefficientPrice());
        result.setUpstreamChannelPrice(priceSnapshot.upstreamChannelPrice());
    }

    private void applyChannelPricePolicy(PreCheckResult result, PriceSnapshot priceSnapshot, Channel channel) {
        if (result == null || !result.isPass() || priceSnapshot == null || channel == null) {
            return;
        }
        BigDecimal responsePrice = priceSnapshot.responsePrice();
        if (isWithinChannelPriceRange(responsePrice, channel)) {
            return;
        }
        log.warn("[APPLY] filtered by channel price range, channelCode={}, productId={}, price={}, minPrice={}, maxPrice={}, priceReturnMode={}",
                channel.getChannelCode(), result.getProductId(), responsePrice,
                channel.getMinPrice(), channel.getMaxPrice(), channel.getPriceReturnMode());
        result.setPass(false);
        result.setRejectReason(CHANNEL_PRICE_OUT_OF_RANGE);
    }

    private boolean hasChannelPriceRejection(List<PreCheckResult> results) {
        return results != null && results.stream()
                .filter(Objects::nonNull)
                .anyMatch(result -> CHANNEL_PRICE_OUT_OF_RANGE.equals(result.getRejectReason()));
    }

    private boolean isWithinChannelPriceRange(BigDecimal price, Channel channel) {
        if (price == null || channel == null) {
            return true;
        }
        if (channel.getMinPrice() != null && price.compareTo(channel.getMinPrice()) < 0) {
            return false;
        }
        return channel.getMaxPrice() == null || price.compareTo(channel.getMaxPrice()) <= 0;
    }

    private boolean isAfterProfitPriceMode(Channel channel) {
        return channel != null && PRICE_RETURN_MODE_AFTER_PROFIT.equalsIgnoreCase(channel.getPriceReturnMode());
    }

    private BigDecimal resolveDownstreamPrice(PreCheckResult result) {
        if (result == null) {
            return null;
        }
        return result.getDownstreamPrice() != null ? result.getDownstreamPrice() : result.getPrice();
    }

    private BigDecimal multiplyPrice(BigDecimal price, BigDecimal multiplier) {
        if (price == null || multiplier == null) {
            return null;
        }
        return price.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        return price == null ? null : price.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultDecimal(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value;
    }

    private record PriceSnapshot(BigDecimal downstreamPrice,
                                 BigDecimal productCoefficientPrice,
                                 BigDecimal upstreamChannelPrice,
                                 BigDecimal responsePrice) {
    }

    private PreCheckRequest createPreCheckRequest(StandardApplyData data) {
        PreCheckRequest req = new PreCheckRequest();
        req.setPhoneMd5(data.getPhoneMd5());
        req.setPhone(data.getPhone());
        req.setIdCard(data.getIdCard());
        req.setIdCardPrefixFour(data.getIdCardPrefixFour());
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

    private PreCheckRequest createMobileEightPreCheckRequest(StandardApplyData data, String requestId,
                                                             String mobileEight, String fallbackRequestId) {
        PreCheckRequest req = createPreCheckRequest(data);
        req.setRequestId(StringUtils.hasText(requestId) ? requestId.trim() : fallbackRequestId);
        req.setMobileEight(StringUtils.hasText(mobileEight) ? mobileEight.trim() : resolveMobileEight(data.getPhone()));
        req.setIp(data.getIp());
        return req;
    }

    private PreCheckRequest copyPreCheckRequest(PreCheckRequest source) {
        PreCheckRequest target = new PreCheckRequest();
        target.setRequestId(source.getRequestId());
        target.setPhone(source.getPhone());
        target.setMobileEight(source.getMobileEight());
        target.setIdCard(source.getIdCard());
        target.setName(source.getName());
        target.setPhoneMd5(source.getPhoneMd5());
        target.setIdCardMd5(source.getIdCardMd5());
        target.setIdCardPrefixFour(source.getIdCardPrefixFour());
        target.setAge(source.getAge());
        target.setCityCode(source.getCityCode());
        target.setWorkCity(source.getWorkCity());
        target.setIp(source.getIp());
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

    private String resolveMobileEight(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        String trimmed = phone.trim();
        return trimmed.length() <= 8 ? trimmed : trimmed.substring(trimmed.length() - 8);
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

}
