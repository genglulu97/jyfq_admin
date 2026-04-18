package com.jyfq.loan.service.upstream;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.ResultCode;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.mapper.InstitutionProductMapper;
import com.jyfq.loan.model.dto.CommonUpstreamEnvelopeDTO;
import com.jyfq.loan.model.dto.CommonUpstreamPayloadDTO;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.model.entity.Channel;
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

        PushResult pushResult = applyService.pushToInstitution(applyData, payload.getProductId());
        return buildApplyResponse(scene, channel.getChannelCode(), payload.getProductId(), pushResult);
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
        if (payload.getProductId() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "productId");
        }
        if (!StringUtils.hasText(payload.getName())) {
            throw new BizException(ResultCode.PARAM_MISSING, "name");
        }
        if (!StringUtils.hasText(payload.getPhone())) {
            throw new BizException(ResultCode.PARAM_MISSING, "phone");
        }
        if (!StringUtils.hasText(payload.getIdCard())) {
            throw new BizException(ResultCode.PARAM_MISSING, "idCard");
        }
    }

    private void enrichExtraInfo(StandardApplyData applyData, CommonUpstreamPayloadDTO payload, String scene, String orgCode) {
        Map<String, Object> extraInfo = applyData.getExtraInfo() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(applyData.getExtraInfo());
        extraInfo.put("scene", scene);
        extraInfo.put("orgCode", orgCode);
        if (payload.getProductId() != null) {
            extraInfo.put("productId", payload.getProductId());
        }
        if (StringUtils.hasText(payload.getAgreeProtocol())) {
            extraInfo.put("agreeProtocol", payload.getAgreeProtocol().trim());
        }
        applyData.setExtraInfo(extraInfo);
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
        response.put("message", pushResult == null ? "push failed" : pushResult.getMsg());
        return response;
    }
}
