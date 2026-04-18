package com.jyfq.loan.service.upstream;

import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.ResultCode;
import com.jyfq.loan.common.util.AesUtil;
import com.jyfq.loan.model.entity.Channel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class ChannelCryptoService {

    public String decrypt(Channel channel, String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            throw new BizException(ResultCode.PARAM_MISSING, "data");
        }
        String encryptType = normalizeEncryptType(channel);
        if ("PLAIN".equals(encryptType)) {
            return cipherText;
        }
        if (!"AES".equals(encryptType)) {
            throw new BizException(ResultCode.PARAM_ERROR, "unsupported encryptType: " + encryptType);
        }
        return AesUtil.decryptByTransformation(cipherText, channel.getAppKey(), buildTransformation(channel), resolveIv(channel));
    }

    public String encrypt(Channel channel, String plainText) {
        String encryptType = normalizeEncryptType(channel);
        if ("PLAIN".equals(encryptType)) {
            return plainText;
        }
        if (!"AES".equals(encryptType)) {
            throw new BizException(ResultCode.PARAM_ERROR, "unsupported encryptType: " + encryptType);
        }
        return AesUtil.encryptByTransformation(plainText, channel.getAppKey(), buildTransformation(channel), resolveIv(channel));
    }

    private String normalizeEncryptType(Channel channel) {
        String encryptType = StringUtils.hasText(channel.getEncryptType()) ? channel.getEncryptType().trim() : "AES";
        return encryptType.toUpperCase(Locale.ROOT);
    }

    private String buildTransformation(Channel channel) {
        String encryptType = normalizeEncryptType(channel);
        if ("PLAIN".equals(encryptType)) {
            return "PLAIN";
        }
        String cipherMode = StringUtils.hasText(channel.getCipherMode()) ? channel.getCipherMode().trim() : "ECB";
        String paddingMode = StringUtils.hasText(channel.getPaddingMode()) ? channel.getPaddingMode().trim() : "PKCS5Padding";
        return encryptType + "/" + cipherMode.toUpperCase(Locale.ROOT) + "/" + paddingMode;
    }

    private String resolveIv(Channel channel) {
        if (StringUtils.hasText(channel.getIvValue())) {
            return channel.getIvValue().trim();
        }
        String appKey = channel.getAppKey();
        if (!StringUtils.hasText(appKey) || appKey.length() < 16) {
            throw new BizException(ResultCode.DECRYPT_ERROR, "channel appKey invalid");
        }
        return appKey.substring(0, 16);
    }
}
