package com.jyfq.loan.service.strategy.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.ResultCode;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.service.strategy.UpstreamStrategy;
import com.jyfq.loan.service.upstream.ChannelCryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MicroSilverUpstreamStrategy implements UpstreamStrategy {

    private final ChannelMapper channelMapper;
    private final ChannelCryptoService channelCryptoService;

    @Override
    public String getChannelCode() {
        return "microsilver";
    }

    @Override
    public StandardApplyData parseRequest(String rawData) {
        if (!StringUtils.hasText(rawData)) {
            throw new BizException(ResultCode.PARAM_MISSING, "data");
        }

        Channel channel = channelMapper.selectOne(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelCode, getChannelCode()));
        if (channel == null) {
            throw new BizException(ResultCode.CHANNEL_NOT_FOUND, getChannelCode());
        }
        if (!Integer.valueOf(1).equals(channel.getStatus())) {
            throw new BizException(ResultCode.CHANNEL_DISABLED, getChannelCode());
        }
        if (!StringUtils.hasText(channel.getAppKey())) {
            throw new BizException(ResultCode.DECRYPT_ERROR, "channel appKey missing");
        }

        String decrypted;
        JSONObject json;
        try {
            decrypted = channelCryptoService.decrypt(channel, rawData);
            json = JSON.parseObject(decrypted);
        } catch (Exception e) {
            throw new BizException(ResultCode.DECRYPT_ERROR, "AES/ECB decrypt failed");
        }

        if (json == null || json.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "解密后请求数据为空");
        }

        validateRequiredFields(json);

        String phoneMd5 = json.getString("phoneMd5");
        String phone = json.getString("phone");
        log.info("[微银预检] 解密前, 手机号MD5={}, 手机号={}, 渠道号={}, 密文数据={}",
                phoneMd5, maskPhone(phone), getChannelCode(), rawData);
        log.info("[微银预检] 解密后明文, 手机号MD5={}, 手机号={}, 渠道号={}, 请求数据={}",
                phoneMd5, maskPhone(phone), getChannelCode(), buildMaskedPayload(json));

        return StandardApplyData.builder()
                .channelCode(getChannelCode())
                .phoneMd5(json.getString("phoneMd5"))
                .name(json.getString("name"))
                .phone(json.getString("phone"))
                .idCard(json.getString("idCard"))
                .age(json.getInteger("age"))
                .cityCode(json.getString("cityCode"))
                .gender(json.getInteger("gender"))
                .profession(json.getInteger("profession"))
                .zhima(json.getInteger("zhima"))
                .house(json.getInteger("house"))
                .vehicle(json.getInteger("vehicle"))
                .providentFund(json.getInteger("providentFund"))
                .socialSecurity(json.getInteger("socialSecurity"))
                .commercialInsurance(json.getInteger("commercialInsurance"))
                .overdue(json.getInteger("overdue"))
                .loanAmount(json.getInteger("loanAmount"))
                .loanTime(json.getInteger("loanTime"))
                .ip(json.getString("ip"))
                .build();
    }

    @Override
    public String encryptResponse(Object result) {
        Channel channel = channelMapper.selectOne(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelCode, getChannelCode()));
        if (channel == null) {
            throw new BizException(ResultCode.CHANNEL_NOT_FOUND, getChannelCode());
        }
        if (!StringUtils.hasText(channel.getAppKey())) {
            throw new BizException(ResultCode.DECRYPT_ERROR, "channel appKey missing");
        }
        String jsonResult = JSON.toJSONString(result);
        return channelCryptoService.encrypt(channel, jsonResult);
    }

    private void validateRequiredFields(JSONObject json) {
        validateRequiredField(json, "phoneMd5");
        validateRequiredField(json, "phone");
        validateRequiredField(json, "name");
        validateRequiredField(json, "idCard");
        validateRequiredField(json, "age");
        validateRequiredField(json, "cityCode");
        validateRequiredField(json, "loanAmount");
    }

    private void validateRequiredField(JSONObject json, String fieldName) {
        Object value = json.get(fieldName);
        if (value == null) {
            throw new BizException(ResultCode.PARAM_MISSING, fieldName);
        }
        if (value instanceof String && !StringUtils.hasText((String) value)) {
            throw new BizException(ResultCode.PARAM_MISSING, fieldName);
        }
    }

    private String buildMaskedPayload(JSONObject json) {
        JSONObject masked = new JSONObject(json);
        masked.put("phone", maskPhone(json.getString("phone")));
        masked.put("idCard", maskIdCard(json.getString("idCard")));
        return JSON.toJSONString(masked);
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }
}
