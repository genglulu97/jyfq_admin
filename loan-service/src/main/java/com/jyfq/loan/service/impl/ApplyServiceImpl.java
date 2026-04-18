package com.jyfq.loan.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jyfq.loan.common.util.AesUtil;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.mapper.InstitutionProductMapper;
import com.jyfq.loan.mapper.PushRecordMapper;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.model.entity.ApplyOrder;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.entity.InstitutionProduct;
import com.jyfq.loan.model.entity.PushRecord;
import com.jyfq.loan.service.ApplyService;
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
    private final ChannelMapper channelMapper;
    private final PushRecordMapper pushRecordMapper;
    private final InstitutionProductMapper institutionProductMapper;

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

        ApplyOrder order = createAndSaveOrder(data, channel);
        List<InstitutionProduct> matchedProducts = matchService.findMatchedProducts(data);
        log.info("[APPLY] matched products, orderNo={}, products={}", order.getOrderNo(), buildMatchedProductLog(matchedProducts));

        if (matchedProducts.isEmpty()) {
            updateOrderSnapshot(order.getOrderNo(), null, null, null, null, 9, "no matched product", null);
            return PreCheckResult.builder().pass(false).rejectReason("no matched product").build();
        }

        PreCheckRequest basePreCheckReq = createPreCheckRequest(data);
        List<CompletableFuture<PreCheckResult>> futures = matchedProducts.stream()
                .map(product -> CompletableFuture.supplyAsync(() -> preCheckSingleProduct(order, product, basePreCheckReq), collisionExecutor)
                        .completeOnTimeout(null, 3, TimeUnit.SECONDS))
                .collect(Collectors.toList());

        List<PreCheckResult> results = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .filter(PreCheckResult::isPass)
                .filter(r -> r.getPrice() != null)
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            updateOrderSnapshot(order.getOrderNo(), null, null, null, null, 9, "all institutions rejected", null);
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
            updateOrderSnapshot(order.getOrderNo(), winner.getInstId(), winner.getProductId(),
                    winnerProduct == null ? null : winnerProduct.getProductName(), null, 0, null, winner.getPrice());
            log.info("[APPLY] winner selected, orderNo={}, instCode={}, productId={}, price={}",
                    order.getOrderNo(), winner.getInstCode(), winner.getProductId(), winner.getPrice());
        }

        return winner;
    }

    @Override
    public PushResult pushToInstitution(StandardApplyData data, Long productId) {
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

        String traceId = UUID.randomUUID().toString().replace("-", "");
        String orderNo = "P" + System.currentTimeMillis();

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

        savePushExecution(orderNo, traceId, product, inst, result, (int) cost);
        return result;
    }

    private ApplyOrder createAndSaveOrder(StandardApplyData data, Channel channel) {
        String traceId = UUID.randomUUID().toString().replace("-", "");

        ApplyOrder order = new ApplyOrder();
        order.setOrderNo(String.valueOf(System.currentTimeMillis()));
        order.setChannelId(channel.getId());
        order.setChannelCode(channel.getChannelCode());
        order.setTraceId(traceId);
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
        order.setOrderStatus(0);
        applyOrderMapper.insert(order);
        return order;
    }

    private PreCheckResult preCheckSingleProduct(ApplyOrder order, InstitutionProduct product, PreCheckRequest basePreCheckReq) {
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
            log.info("[APPLY] dispatch preCheck orderNo={} productId={} productName={} instId={} instName={} instCode={} preCheckUrl={}",
                    order.getOrderNo(),
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
                savePreCheckRecord(order, product, inst, result, (int) cost);
            }
            return result;
        } catch (Exception e) {
            log.error("[APPLY] pre-check failed, productId={}, orderNo={}", product.getId(), order.getOrderNo(), e);
            return null;
        }
    }

    private void savePreCheckRecord(ApplyOrder order, InstitutionProduct product, Institution inst, PreCheckResult result, int cost) {
        PushRecord record = new PushRecord();
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setChannelId(order.getChannelId());
        record.setInstId(inst.getId());
        record.setInstCode(inst.getInstCode());
        record.setProductId(product.getId());
        record.setTraceId(order.getTraceId());
        record.setRequestId(result.getUuid());
        record.setPushStatus(result.isPass() ? 2 : 4);
        record.setResponseLog(JSON.toJSONString(result));
        record.setErrorMsg(result.getRejectReason());
        record.setCostMs(cost);
        record.setPushedAt(LocalDateTime.now());
        pushRecordMapper.insert(record);
    }

    private void savePushExecution(String orderNo, String traceId, InstitutionProduct product, Institution inst, PushResult result, int cost) {
        PushRecord record = new PushRecord();
        record.setOrderNo(orderNo);
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
    }

    private void updateOrderSnapshot(String orderNo, Long instId, Long productId, String productNameSnapshot, Long pushId,
                                     int status, String rejectReason, BigDecimal settlementPrice) {
        LambdaUpdateWrapper<ApplyOrder> wrapper = new LambdaUpdateWrapper<ApplyOrder>()
                .eq(ApplyOrder::getOrderNo, orderNo)
                .set(ApplyOrder::getOrderStatus, status);

        if (instId != null) {
            wrapper.set(ApplyOrder::getInstId, instId);
        }
        if (productId != null) {
            wrapper.set(ApplyOrder::getProductId, productId);
        }
        if (productNameSnapshot != null) {
            wrapper.set(ApplyOrder::getProductNameSnapshot, productNameSnapshot);
        }
        if (pushId != null) {
            wrapper.set(ApplyOrder::getPushId, pushId);
        }
        if (rejectReason != null) {
            wrapper.set(ApplyOrder::getRejectReason, rejectReason);
        }
        if (settlementPrice != null) {
            wrapper.set(ApplyOrder::getSettlementPrice, settlementPrice);
        }

        applyOrderMapper.update(null, wrapper);
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
