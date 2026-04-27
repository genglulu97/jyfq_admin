package com.jyfq.loan.service.upstream;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.ResultCode;
import com.jyfq.loan.common.util.AesUtil;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.mapper.InstitutionProductMapper;
import com.jyfq.loan.model.dto.CommonUpstreamEnvelopeDTO;
import com.jyfq.loan.model.dto.CommonUpstreamPayloadDTO;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.entity.CollisionRecord;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.entity.InstitutionProduct;
import com.jyfq.loan.service.ApplyService;
import com.jyfq.loan.thirdparty.model.PreCheckResult;
import com.jyfq.loan.thirdparty.model.PushResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonUpstreamGatewayService {

    private static final Set<String> SUPPORTED_SCENES = Set.of("institution", "masked", "half-process", "full-process");

    private final ChannelMapper channelMapper;
    private final InstitutionMapper institutionMapper;
    private final InstitutionProductMapper institutionProductMapper;
    private final ApplyService applyService;
    private final ChannelCryptoService channelCryptoService;

    public Map<String, Object> preCheck(String scene, CommonUpstreamEnvelopeDTO request) {
        validateScene(scene);
        Channel channel = getEnabledChannel(request.getOrgCode());
        CommonUpstreamPayloadDTO payload = decryptPayload(channel, request.getData());
        validatePreCheckPayload(payload);

        StandardApplyData applyData = CommonUpstreamMappingUtil.toStandardData(channel.getChannelCode(), payload);
        enrichExtraInfo(applyData, payload, scene, channel.getChannelCode());

        PreCheckResult winner = applyService.competitivePreCheck(applyData);
        return buildPreCheckResponse(scene, channel.getChannelCode(), winner);
    }

    public Map<String, Object> apply(String scene, CommonUpstreamEnvelopeDTO request) {
        validateScene(scene);
        Channel channel = getEnabledChannel(request.getOrgCode());
        CommonUpstreamPayloadDTO payload = decryptPayload(channel, request.getData());
        validateApplyPayload(payload);

        StandardApplyData applyData = CommonUpstreamMappingUtil.toStandardData(channel.getChannelCode(), payload);
        enrichExtraInfo(applyData, payload, scene, channel.getChannelCode());

        CollisionRecord matchedOrder = applyService.findLatestMatchedCollisionRecord(applyData);
        if (matchedOrder == null || matchedOrder.getProductId() == null) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "请先preCheck并撞库通过后再apply");
        }

        validateApplyAgainstMatchedOrder(matchedOrder, payload, applyData, channel);
        PushResult pushResult = applyService.pushToInstitution(applyData, matchedOrder.getProductId(), matchedOrder.getCollisionNo());
        return buildApplyResponse(scene, channel.getChannelCode(), matchedOrder.getProductId(), pushResult);
    }

    private void validateScene(String scene) {
        if (!SUPPORTED_SCENES.contains(scene)) {
            throw new BizException(ResultCode.PARAM_ERROR, "unsupported scene: " + scene);
        }
    }

    private Channel getEnabledChannel(String orgCode) {
        if (!StringUtils.hasText(orgCode)) {
            throw new BizException(ResultCode.PARAM_MISSING, "orgCode");
        }

        Channel channel = channelMapper.selectOne(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelCode, orgCode.trim()));
        if (channel == null) {
            throw new BizException(ResultCode.CHANNEL_NOT_FOUND, orgCode);
        }
        if (!Integer.valueOf(1).equals(channel.getStatus())) {
            throw new BizException(ResultCode.CHANNEL_DISABLED, orgCode);
        }
        if (!StringUtils.hasText(channel.getAppKey())) {
            throw new BizException(ResultCode.DECRYPT_ERROR, "channel appKey missing");
        }
        return channel;
    }

    private CommonUpstreamPayloadDTO decryptPayload(Channel channel, String encryptedData) {
        if (!StringUtils.hasText(encryptedData)) {
            throw new BizException(ResultCode.PARAM_MISSING, "data");
        }
        try {
            String decrypted = channelCryptoService.decrypt(channel, encryptedData);
            CommonUpstreamPayloadDTO payload = JSON.parseObject(decrypted, CommonUpstreamPayloadDTO.class);
            if (payload == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "empty payload");
            }
            return payload;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[COMMON-UPSTREAM] decrypt failed, orgCode={}", channel.getChannelCode(), ex);
            throw new BizException(ResultCode.DECRYPT_ERROR, "AES/ECB decrypt failed");
        }
    }

    private void validatePreCheckPayload(CommonUpstreamPayloadDTO payload) {
        if (payload == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "payload is required");
        }
        if (!StringUtils.hasText(payload.getPhone()) && !StringUtils.hasText(payload.getPhoneMd5())) {
            throw new BizException(ResultCode.PARAM_MISSING, "phone or phoneMd5");
        }
        if (payload.getAge() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "age");
        }
        if (!StringUtils.hasText(payload.getCityCode()) && !StringUtils.hasText(payload.getCity())) {
            throw new BizException(ResultCode.PARAM_MISSING, "cityCode or city");
        }
        if (payload.getLoanAmount() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "loanAmount");
        }
    }

    private void validateApplyPayload(CommonUpstreamPayloadDTO payload) {
        validatePreCheckPayload(payload);
        if (!StringUtils.hasText(payload.getName())) {
            throw new BizException(ResultCode.PARAM_MISSING, "name");
        }
    }

    private void enrichExtraInfo(StandardApplyData applyData, CommonUpstreamPayloadDTO payload, String scene, String orgCode) {
        Map<String, Object> extraInfo = applyData.getExtraInfo() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(applyData.getExtraInfo());
        extraInfo.put("scene", scene);
        extraInfo.put("orgCode", orgCode);
        extraInfo.put("upstreamPayload", buildUpstreamPayloadSnapshot(payload));
        if (payload.getProductId() != null) {
            extraInfo.put("productId", payload.getProductId());
        }
        if (StringUtils.hasText(payload.getAgreeProtocol())) {
            extraInfo.put("agreeProtocol", payload.getAgreeProtocol().trim());
        }
        applyData.setExtraInfo(extraInfo);
    }

    private Map<String, Object> buildUpstreamPayloadSnapshot(CommonUpstreamPayloadDTO payload) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (payload == null) {
            return snapshot;
        }
        snapshot.put("name", payload.getName());
        snapshot.put("phone", payload.getPhone());
        snapshot.put("phoneMd5", payload.getPhoneMd5());
        snapshot.put("idCard", payload.getIdCard());
        snapshot.put("age", payload.getAge());
        snapshot.put("city", payload.getCity());
        snapshot.put("cityCode", payload.getCityCode());
        snapshot.put("province", payload.getProvince());
        snapshot.put("provinceCode", payload.getProvinceCode());
        snapshot.put("gender", payload.getGender());
        snapshot.put("loanTime", payload.getLoanTime());
        snapshot.put("profession", payload.getProfession());
        snapshot.put("zhima", payload.getZhima());
        snapshot.put("providentFund", payload.getProvidentFund());
        snapshot.put("socialSecurity", payload.getSocialSecurity());
        snapshot.put("commercialInsurance", payload.getCommercialInsurance());
        snapshot.put("house", payload.getHouse());
        snapshot.put("overdue", payload.getOverdue());
        snapshot.put("vehicle", payload.getVehicle());
        snapshot.put("loanAmount", payload.getLoanAmount());
        snapshot.put("deviceIp", payload.getDeviceIp());
        snapshot.put("customerLevel", payload.getCustomerLevel());
        snapshot.put("productId", payload.getProductId());
        snapshot.put("agreeProtocol", payload.getAgreeProtocol());
        snapshot.put("workCity", payload.getWorkCity());
        return snapshot;
    }

    private void validateApplyAgainstMatchedOrder(CollisionRecord matchedOrder, CommonUpstreamPayloadDTO payload,
                                                  StandardApplyData applyData, Channel channel) {
        Map<String, Object> storedSnapshot = parseStoredUpstreamPayload(matchedOrder);
        if (!storedSnapshot.isEmpty()) {
            compareField("name", storedSnapshot.get("name"), payload == null ? null : payload.getName());
            compareField("phone", storedSnapshot.get("phone"), payload == null ? null : payload.getPhone());
            compareField("phoneMd5", storedSnapshot.get("phoneMd5"), payload == null ? null : payload.getPhoneMd5());
            compareField("idCard", storedSnapshot.get("idCard"), payload == null ? null : payload.getIdCard());
            compareField("age", storedSnapshot.get("age"), payload == null ? null : payload.getAge());
            compareField("city", storedSnapshot.get("city"), payload == null ? null : payload.getCity());
            compareField("cityCode", storedSnapshot.get("cityCode"), payload == null ? null : payload.getCityCode());
            compareField("province", storedSnapshot.get("province"), payload == null ? null : payload.getProvince());
            compareField("provinceCode", storedSnapshot.get("provinceCode"), payload == null ? null : payload.getProvinceCode());
            compareField("gender", storedSnapshot.get("gender"), payload == null ? null : payload.getGender());
            compareField("loanTime", storedSnapshot.get("loanTime"), payload == null ? null : payload.getLoanTime());
            compareField("profession", storedSnapshot.get("profession"), payload == null ? null : payload.getProfession());
            compareField("zhima", storedSnapshot.get("zhima"), payload == null ? null : payload.getZhima());
            compareField("providentFund", storedSnapshot.get("providentFund"), payload == null ? null : payload.getProvidentFund());
            compareField("socialSecurity", storedSnapshot.get("socialSecurity"), payload == null ? null : payload.getSocialSecurity());
            compareField("commercialInsurance", storedSnapshot.get("commercialInsurance"), payload == null ? null : payload.getCommercialInsurance());
            compareField("house", storedSnapshot.get("house"), payload == null ? null : payload.getHouse());
            compareField("overdue", storedSnapshot.get("overdue"), payload == null ? null : payload.getOverdue());
            compareField("vehicle", storedSnapshot.get("vehicle"), payload == null ? null : payload.getVehicle());
            compareField("loanAmount", storedSnapshot.get("loanAmount"), payload == null ? null : payload.getLoanAmount());
            compareField("deviceIp", storedSnapshot.get("deviceIp"), payload == null ? null : payload.getDeviceIp());
            compareField("customerLevel", storedSnapshot.get("customerLevel"), payload == null ? null : payload.getCustomerLevel());
            compareField("agreeProtocol", storedSnapshot.get("agreeProtocol"), payload == null ? null : payload.getAgreeProtocol());
            compareField("workCity", storedSnapshot.get("workCity"), payload == null ? null : payload.getWorkCity());
            return;
        }

        log.warn("[COMMON-UPSTREAM] matched collision missing upstream payload snapshot, fallback to normalized comparison, collisionNo={}",
                matchedOrder.getCollisionNo());
        compareField("phoneMd5", matchedOrder.getPhoneMd5(), applyData.getPhoneMd5());
        compareField("name", decryptOrderField(matchedOrder.getUserName(), channel), applyData.getName());
        compareField("phone", decryptOrderField(matchedOrder.getPhoneEnc(), channel), applyData.getPhone());
        compareField("idCard", decryptOrderField(matchedOrder.getIdCardEnc(), channel), applyData.getIdCard());
        compareField("age", matchedOrder.getAge(), applyData.getAge());
        compareField("cityCode", matchedOrder.getCityCode(), applyData.getCityCode());
        compareField("workCity", matchedOrder.getWorkCity(), applyData.getWorkCity());
        compareField("gender", matchedOrder.getGender(), applyData.getGender());
        compareField("profession", matchedOrder.getProfession(), applyData.getProfession());
        compareField("zhima", matchedOrder.getZhima(), applyData.getZhima());
        compareField("house", matchedOrder.getHouse(), applyData.getHouse());
        compareField("vehicle", matchedOrder.getVehicle(), applyData.getVehicle());
        compareField("vehicleStatus", matchedOrder.getVehicleStatus(), applyData.getVehicleStatus());
        compareField("vehicleValue", matchedOrder.getVehicleValue(), applyData.getVehicleValue());
        compareField("providentFund", matchedOrder.getProvidentFund(), applyData.getProvidentFund());
        compareField("socialSecurity", matchedOrder.getSocialSecurity(), applyData.getSocialSecurity());
        compareField("commercialInsurance", matchedOrder.getCommercialInsurance(), applyData.getCommercialInsurance());
        compareField("overdue", matchedOrder.getOverdue(), applyData.getOverdue());
        compareField("loanAmount", matchedOrder.getLoanAmount(), applyData.getLoanAmount());
        compareField("loanTime", matchedOrder.getLoanTime(), applyData.getLoanTime());
    }

    private Map<String, Object> parseStoredUpstreamPayload(CollisionRecord matchedOrder) {
        if (matchedOrder == null || !StringUtils.hasText(matchedOrder.getExtJson())) {
            return java.util.Collections.emptyMap();
        }
        try {
            Map<String, Object> extJson = JSON.parseObject(matchedOrder.getExtJson(), Map.class);
            if (extJson == null) {
                return java.util.Collections.emptyMap();
            }
            Object upstreamPayload = extJson.get("upstreamPayload");
            if (upstreamPayload instanceof Map<?, ?> upstreamMap) {
                Map<String, Object> result = new LinkedHashMap<>();
                upstreamMap.forEach((key, value) -> result.put(String.valueOf(key), value));
                return result;
            }
        } catch (Exception ex) {
            log.warn("[COMMON-UPSTREAM] parse upstream payload snapshot failed, collisionNo={}", matchedOrder.getCollisionNo(), ex);
        }
        return java.util.Collections.emptyMap();
    }

    private String decryptOrderField(String cipherText, Channel channel) {
        if (!StringUtils.hasText(cipherText) || channel == null || !StringUtils.hasText(channel.getAppKey())) {
            return null;
        }
        try {
            String value = AesUtil.decrypt(cipherText, channel.getAppKey());
            return StringUtils.hasText(value) ? value : null;
        } catch (Exception ex) {
            log.warn("[COMMON-UPSTREAM] decrypt matched order field failed, channelCode={}", channel.getChannelCode(), ex);
            return null;
        }
    }

    private void compareField(String fieldName, Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "撞库记录与进件参数不一致: " + fieldName);
        }
    }

    private Map<String, Object> buildPreCheckResponse(String scene, String orgCode, PreCheckResult winner) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("scene", scene);
        response.put("orgCode", orgCode);

        if (winner == null || !winner.isPass()) {
            response.put("matched", false);
            response.put("success", false);
            response.put("message", winner == null ? "no matched product" : winner.getRejectReason());
            return response;
        }

        Institution inst = winner.getInstId() == null ? null : institutionMapper.selectById(winner.getInstId());
        InstitutionProduct product = winner.getProductId() == null ? null : institutionProductMapper.selectById(winner.getProductId());

        response.put("matched", true);
        response.put("success", true);
        response.put("message", "matched");
        response.put("instId", winner.getInstId());
        response.put("instCode", winner.getInstCode());
        response.put("instName", inst == null ? null : inst.getInstName());
        response.put("merchantAlias", inst == null ? null : inst.getMerchantAlias());
        response.put("productId", winner.getProductId());
        response.put("orderId", winner.getOrderId());
        response.put("productName", StringUtils.hasText(winner.getProductName()) ? winner.getProductName() : product == null ? null : product.getProductName());
        response.put("productLogo", StringUtils.hasText(winner.getProductLogo()) ? winner.getProductLogo() : product == null ? null : product.getProductIcon());
        response.put("companyName", StringUtils.hasText(winner.getCompanyName()) ? winner.getCompanyName() : inst == null ? null : inst.getInstName());
        response.put("protocolUrl", product == null ? null : product.getProtocolUrl());
        response.put("protocolList", winner.getProtocolList());
        response.put("price", winner.getPrice() == null ? null : winner.getPrice().toPlainString());
        response.put("requestId", winner.getUuid());
        return response;
    }

    private Map<String, Object> buildApplyResponse(String scene, String orgCode, Long productId, PushResult pushResult) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("scene", scene);
        response.put("orgCode", orgCode);
        response.put("productId", productId);
        response.put("success", pushResult != null && pushResult.isSuccess());
        response.put("instCode", pushResult == null ? null : pushResult.getInstCode());
        response.put("thirdOrderNo", pushResult == null ? null : pushResult.getThirdOrderNo());
        response.put("message", normalizeApplyMessage(pushResult == null ? null : pushResult.getMsg()));
        return response;
    }

    private String normalizeApplyMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "进件失败";
        }

        String trimmed = message.trim();
        if ("push failed".equalsIgnoreCase(trimmed)) {
            return "进件失败";
        }
        if ("no matched product".equalsIgnoreCase(trimmed)) {
            return "未匹配到产品";
        }
        if (trimmed.startsWith("downstream error:")) {
            String detail = trimmed.substring("downstream error:".length()).trim();
            if (!StringUtils.hasText(detail)) {
                return "下游机构处理失败";
            }
            if (containsChinese(detail)) {
                return detail;
            }
            return "下游机构处理失败: " + detail;
        }
        return trimmed;
    }

    private boolean containsChinese(String value) {
        for (int i = 0; i < value.length(); i++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(value.charAt(i));
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                return true;
            }
        }
        return false;
    }
}
