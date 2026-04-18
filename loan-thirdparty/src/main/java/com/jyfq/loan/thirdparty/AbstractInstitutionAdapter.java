package com.jyfq.loan.thirdparty;

import com.alibaba.fastjson2.JSON;
import com.jyfq.loan.common.exception.ThirdPartyException;
import com.jyfq.loan.common.util.AesUtil;
import com.jyfq.loan.model.entity.Institution;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Shared downstream adapter HTTP/encryption support.
 */
@Slf4j
public abstract class AbstractInstitutionAdapter implements InstitutionAdapter {

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    protected <T> T doPost(Institution institution, String url, Object body, Class<T> respType) {
        String requestJson = JSON.toJSONString(body);
        String encryptedBody = encrypt(institution, requestJson);
        long startMs = System.currentTimeMillis();
        log.info("[PUSH] 下游请求准备 | 适配器={} | 机构编码={} | 请求地址={} | 加密方式={} | 加密Key={} | 未加密请求JSON={} | 撞库请求加密JSON={}",
                getAdapterKey(),
                institution == null ? null : institution.getInstCode(),
                url,
                institution == null ? null : institution.getEncryptType(),
                institution == null ? null : institution.getAppKey(),
                requestJson,
                encryptedBody);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(encryptedBody, JSON_MEDIA))
                    .build();

            try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                String rawResp = response.body() != null ? response.body().string() : "";
                String decrypted = decrypt(institution, rawResp);
                long costMs = System.currentTimeMillis() - startMs;
                log.info("[PUSH] 下游请求成功 | 适配器={} | 机构编码={} | 请求地址={} | 耗时={}ms | 原始响应={} | 解密后响应={}",
                        getAdapterKey(),
                        institution == null ? null : institution.getInstCode(),
                        url,
                        costMs,
                        rawResp,
                        decrypted);
                return JSON.parseObject(StringUtils.hasText(decrypted) ? decrypted : "{}", respType);
            }
        } catch (IOException e) {
            long costMs = System.currentTimeMillis() - startMs;
            log.error("[PUSH] adapter={} instCode={} url={} costMs={} error={}",
                    getAdapterKey(), institution == null ? null : institution.getInstCode(), url, costMs, e.getMessage());
            throw new ThirdPartyException(institution == null ? getAdapterKey() : institution.getInstCode(), e);
        }
    }

    protected <T> T doPlainPost(Institution institution, String url, Object body, Class<T> respType) {
        String requestJson = JSON.toJSONString(body);
        long startMs = System.currentTimeMillis();
        log.info("[PUSH] 下游明文请求准备 | 适配器={} | 机构编码={} | 请求地址={} | 未加密请求JSON={}",
                getAdapterKey(),
                institution == null ? null : institution.getInstCode(),
                url,
                requestJson);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestJson, JSON_MEDIA))
                    .build();

            try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                String rawResp = response.body() != null ? response.body().string() : "";
                long costMs = System.currentTimeMillis() - startMs;
                log.info("[PUSH] 下游明文请求成功 | 适配器={} | 机构编码={} | 请求地址={} | 耗时={}ms | 原始响应={}",
                        getAdapterKey(), institution == null ? null : institution.getInstCode(), url, costMs, rawResp);
                return JSON.parseObject(StringUtils.hasText(rawResp) ? rawResp : "{}", respType);
            }
        } catch (IOException e) {
            long costMs = System.currentTimeMillis() - startMs;
            log.error("[PUSH] adapter={} instCode={} url={} costMs={} error={} plain",
                    getAdapterKey(), institution == null ? null : institution.getInstCode(), url, costMs, e.getMessage());
            throw new ThirdPartyException(institution == null ? getAdapterKey() : institution.getInstCode(), e);
        }
    }

    protected String encrypt(Institution institution, String plainText) {
        if (institution == null || !StringUtils.hasText(institution.getEncryptType()) || !StringUtils.hasText(institution.getAppKey())) {
            return plainText;
        }
        String encryptType = institution.getEncryptType().trim().toUpperCase();
        return switch (encryptType) {
            case "AES", "AES_CBC", "CBC" -> AesUtil.encrypt(plainText, institution.getAppKey());
            case "AES_ECB", "ECB" -> AesUtil.encryptECB(plainText, institution.getAppKey());
            default -> plainText;
        };
    }

    protected String decrypt(Institution institution, String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return cipherText;
        }
        if (institution == null || !StringUtils.hasText(institution.getEncryptType()) || !StringUtils.hasText(institution.getAppKey())) {
            return cipherText;
        }
        String encryptType = institution.getEncryptType().trim().toUpperCase();
        try {
            return switch (encryptType) {
                case "AES", "AES_CBC", "CBC" -> AesUtil.decrypt(cipherText, institution.getAppKey());
                case "AES_ECB", "ECB" -> AesUtil.decryptECB(cipherText, institution.getAppKey());
                default -> cipherText;
            };
        } catch (RuntimeException ex) {
            log.warn("[PUSH] decrypt fallback to raw response, instCode={}",
                    institution == null ? null : institution.getInstCode(), ex);
            return cipherText;
        }
    }
}
