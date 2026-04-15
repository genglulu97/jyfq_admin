package com.jyfq.loan.thirdparty;

import com.alibaba.fastjson2.JSON;
import com.jyfq.loan.common.exception.ThirdPartyException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 机构适配抽象基类
 * <p>提取公共逻辑：HTTP调用、加解密、日志记录、耗时统计</p>
 */
@Slf4j
public abstract class AbstractInstitutionAdapter implements InstitutionAdapter {

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    /**
     * 发送 POST 请求（含加解密 + 日志）
     */
    protected <T> T doPost(String url, Object body, Class<T> respType) {
        String requestJson = JSON.toJSONString(body);
        String encrypted = encrypt(requestJson);
        long startMs = System.currentTimeMillis();

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(encrypted, JSON_MEDIA))
                    .build();

            try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                String rawResp = response.body() != null ? response.body().string() : "";
                String decrypted = decrypt(rawResp);

                long costMs = System.currentTimeMillis() - startMs;
                log.info("[PUSH] inst={} url={} costMs={} success", getInstCode(), url, costMs);

                return JSON.parseObject(decrypted, respType);
            }
        } catch (IOException e) {
            long costMs = System.currentTimeMillis() - startMs;
            log.error("[PUSH] inst={} url={} costMs={} error={}", getInstCode(), url, costMs, e.getMessage());
            throw new ThirdPartyException(getInstCode(), e);
        }
    }

    /**
     * 加密请求报文（各机构自行实现）
     */
    protected abstract String encrypt(String plainText);

    /**
     * 解密响应报文（各机构自行实现）
     */
    protected abstract String decrypt(String cipherText);
}
