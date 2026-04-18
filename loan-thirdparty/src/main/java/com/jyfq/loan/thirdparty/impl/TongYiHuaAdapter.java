package com.jyfq.loan.thirdparty.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.jyfq.loan.common.util.AesUtil;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.thirdparty.AbstractInstitutionAdapter;
import com.jyfq.loan.thirdparty.model.PreCheckRequest;
import com.jyfq.loan.thirdparty.model.PreCheckResult;
import com.jyfq.loan.thirdparty.model.PushRequest;
import com.jyfq.loan.thirdparty.model.PushResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * Config-driven downstream adapter implementation.
 * Protocol: top-level JSON with orgId + data, where data uses AES/ECB/PKCS5Padding.
 */
@Slf4j
@Service("qlqMaskApiPushService")
public class TongYiHuaAdapter extends AbstractInstitutionAdapter {

    @Override
    public String getAdapterKey() {
        return "tongyihua";
    }

    @Override
    public PreCheckResult preCheck(Institution institution, PreCheckRequest req) {
        String preCheckUrl = institution == null ? null : institution.getPreCheckUrl();
        if (!StringUtils.hasText(preCheckUrl)) {
            return PreCheckResult.builder()
                    .pass(false)
                    .instCode(institution == null ? null : institution.getInstCode())
                    .rejectReason("preCheckUrl is not configured")
                    .build();
        }

        JSONObject plainPayload = buildPreCheckPayload(req);
        JSONObject envelope = buildEncryptedEnvelope(institution, plainPayload);
        logWrappedRequest(institution, preCheckUrl, plainPayload, envelope);

        JSONObject resp = doPlainPost(institution, preCheckUrl, envelope, JSONObject.class);
        if (isSuccess(resp)) {
            JSONObject resultData = resolveResultData(resp);
            return PreCheckResult.builder()
                    .pass(true)
                    .instCode(institution.getInstCode())
                    .price(resolvePrice(resultData, resp))
                    .uuid(resolveText(resultData, resp, "uuid", "requestId", "traceId", "orderId"))
                    .orderId(resolveText(resultData, resp, "orderId", "requestId", "traceId"))
                    .productLogo(resolveText(resultData, resp, "productLogo"))
                    .productName(resolveText(resultData, resp, "productName"))
                    .companyName(resolveText(resultData, resp, "companyName"))
                    .protocolList(resolveProtocolList(resultData, resp))
                    .build();
        }

        return PreCheckResult.builder()
                .pass(false)
                .instCode(institution == null ? null : institution.getInstCode())
                .rejectReason(resolveText(resolveResultData(resp), resp, "msg", "message", "errorMsg"))
                .build();
    }

    @Override
    public PushResult push(Institution institution, PushRequest req) {
        if (institution == null || !StringUtils.hasText(institution.getApiPushUrl())) {
            return PushResult.failure("apiPushUrl is not configured");
        }
        log.info("[PUSH] execute downstream push, instCode={}, orderNo={}", institution.getInstCode(), req.getOrderNo());

        try {
            JSONObject plainPayload = buildPushPayload(req);
            JSONObject envelope = buildEncryptedEnvelope(institution, plainPayload);
            logWrappedRequest(institution, institution.getApiPushUrl(), plainPayload, envelope);

            JSONObject resp = doPlainPost(institution, institution.getApiPushUrl(), envelope, JSONObject.class);
            if (isSuccess(resp)) {
                PushResult result = PushResult.success(resolveText(resolveResultData(resp), resp, "msg", "message"));
                result.setInstCode(institution.getInstCode());
                result.setThirdOrderNo(resolveText(resolveResultData(resp), resp, "thirdOrderNo", "orderNo", "applyNo"));
                return result;
            }
            PushResult result = PushResult.failure(resolveText(resolveResultData(resp), resp, "msg", "message", "errorMsg"));
            result.setInstCode(institution.getInstCode());
            return result;
        } catch (Exception e) {
            log.error("[PUSH] downstream push error, instCode={}, orderNo={}", institution.getInstCode(), req.getOrderNo(), e);
            PushResult result = PushResult.failure("downstream error: " + e.getMessage());
            result.setInstCode(institution.getInstCode());
            return result;
        }
    }

    private JSONObject buildPreCheckPayload(PreCheckRequest req) {
        JSONObject payload = new JSONObject();
        payload.put("phoneMd5", normalizePhoneMd5(req.getPhoneMd5(), req.getPhone()));
        payload.put("city", resolveCityName(req.getWorkCity(), req.getCityCode()));
        payload.put("cityCode", req.getCityCode());
        payload.put("age", req.getAge());
        payload.put("gender", normalizeGender(req.getGender()));
        payload.put("loanTime", normalizeLoanTime(req.getLoanTime()));
        payload.put("profession", normalizeProfession(req.getProfession()));
        payload.put("zhima", normalizeZhima(req.getZhima()));
        payload.put("providentFund", normalizeProvidentFund(req.getProvidentFund()));
        payload.put("socialSecurity", normalizeSocialSecurity(req.getSocialSecurity()));
        payload.put("commericalInsurance", normalizeCommercialInsurance(req.getCommercialInsurance()));
        payload.put("house", normalizeHouse(req.getHouse()));
        payload.put("overdue", normalizeOverdue(req.getOverdue()));
        payload.put("vehicle", normalizeVehicle(req.getVehicle()));
        payload.put("loanAmount", normalizeLoanAmount(req.getAmount()));
        return payload;
    }

    private JSONObject buildPushPayload(PushRequest req) {
        JSONObject payload = new JSONObject();
        payload.put("orderNo", req.getOrderNo());
        payload.put("traceId", req.getTraceId());
        payload.put("notifyUrl", req.getNotifyUrl());
        payload.put("productId", req.getProductId());
        payload.put("orderId", req.getOrderId());
        if (req.getStandardData() != null) {
            payload.put("mobile", req.getStandardData().getPhone());
            payload.put("phoneMd5", normalizePhoneMd5(req.getStandardData().getPhoneMd5(), req.getStandardData().getPhone()));
            payload.put("name", req.getStandardData().getName());
            payload.put("idCard", req.getStandardData().getIdCard());
            payload.put("age", req.getStandardData().getAge());
            payload.put("city", resolveCityName(req.getStandardData().getWorkCity(), req.getStandardData().getCityCode()));
            payload.put("cityCode", req.getStandardData().getCityCode());
            payload.put("workCity", req.getStandardData().getWorkCity());
            payload.put("gender", normalizeGender(req.getStandardData().getGender()));
            payload.put("profession", normalizeProfession(req.getStandardData().getProfession()));
            payload.put("zhima", normalizeZhima(req.getStandardData().getZhima()));
            payload.put("house", normalizeHouse(req.getStandardData().getHouse()));
            payload.put("vehicle", normalizeVehicle(req.getStandardData().getVehicle()));
            payload.put("vehicleStatus", req.getStandardData().getVehicleStatus());
            payload.put("vehicleValue", req.getStandardData().getVehicleValue());
            payload.put("providentFund", normalizeProvidentFund(req.getStandardData().getProvidentFund()));
            payload.put("socialSecurity", normalizeSocialSecurity(req.getStandardData().getSocialSecurity()));
            payload.put("commericalInsurance", normalizeCommercialInsurance(req.getStandardData().getCommercialInsurance()));
            payload.put("overdue", normalizeOverdue(req.getStandardData().getOverdue()));
            payload.put("loanAmount", normalizeLoanAmount(req.getStandardData().getLoanAmount()));
            payload.put("loanTime", normalizeLoanTime(req.getStandardData().getLoanTime()));
            payload.put("customerLevel", req.getStandardData().getCustomerLevel());
            payload.put("ip", req.getStandardData().getIp());
        }
        return payload;
    }

    private JSONObject buildEncryptedEnvelope(Institution institution, JSONObject plainPayload) {
        String plainJson = JSON.toJSONString(plainPayload);
        String encryptedData = encryptForTongYiHua(institution, plainJson);
        JSONObject envelope = new JSONObject();
        envelope.put("orgId", resolveOrgId(institution));
        envelope.put("data", encryptedData);
        return envelope;
    }

    private void logWrappedRequest(Institution institution, String url, JSONObject plainPayload, JSONObject envelope) {
        log.info("[PUSH] 下游请求准备 | 适配器={} | 机构编码={} | 请求地址={} | 加密方式=AES/ECB/PKCS5Padding | 加密Key={} | 未加密请求JSON={} | 撞库请求加密JSON={} | 完整下游加密请求JSON={}",
                getAdapterKey(),
                institution == null ? null : institution.getInstCode(),
                url,
                institution == null ? null : institution.getAppKey(),
                JSON.toJSONString(plainPayload),
                envelope.getString("data"),
                JSON.toJSONString(envelope));
    }

    private String encryptForTongYiHua(Institution institution, String plainJson) {
        String appKey = institution == null ? null : institution.getAppKey();
        if (!StringUtils.hasText(appKey)) {
            return plainJson;
        }
        return AesUtil.encryptECB(plainJson, appKey);
    }

    private String resolveOrgId(Institution institution) {
        if (institution == null) {
            return null;
        }
        if (StringUtils.hasText(institution.getBusinessCode())) {
            return institution.getBusinessCode();
        }
        return institution.getInstCode();
    }

    private String normalizePhoneMd5(String phoneMd5, String phone) {
        if (StringUtils.hasText(phoneMd5)) {
            return phoneMd5.trim().toLowerCase();
        }
        return phone;
    }

    private String resolveCityName(String workCity, String cityCode) {
        if (StringUtils.hasText(workCity)) {
            String[] parts = workCity.split("[,/]");
            return parts[parts.length - 1].trim();
        }
        return cityCode;
    }

    private Integer normalizeGender(Integer value) {
        if (value == null) {
            return 0;
        }
        return switch (value) {
            case 1, 2 -> value;
            default -> 0;
        };
    }

    private Integer normalizeLoanTime(Integer value) {
        if (value == null) {
            return 3;
        }
        return switch (value) {
            case 2, 3, 4, 5 -> value;
            case 6 -> 2;
            case 12 -> 3;
            case 24 -> 4;
            case 36 -> 5;
            default -> 3;
        };
    }

    private Integer normalizeProfession(Integer value) {
        if (value == null) {
            return 1;
        }
        return switch (value) {
            case 1, 2, 3, 4 -> value;
            default -> 1;
        };
    }

    private Integer normalizeZhima(Integer value) {
        if (value == null) {
            return 4;
        }
        if (value >= 1 && value <= 5) {
            return value;
        }
        if (value < 600) {
            return 5;
        }
        if (value < 650) {
            return 1;
        }
        if (value < 700) {
            return 2;
        }
        return 3;
    }

    private Integer normalizeProvidentFund(Integer value) {
        if (value == null) {
            return 4;
        }
        return switch (value) {
            case 1, 2, 3 -> value;
            case 0, 4 -> 4;
            default -> 4;
        };
    }

    private Integer normalizeSocialSecurity(Integer value) {
        if (value == null) {
            return 4;
        }
        return switch (value) {
            case 1, 2, 3 -> value;
            case 0, 4 -> 4;
            default -> 4;
        };
    }

    private Integer normalizeCommercialInsurance(Integer value) {
        if (value == null) {
            return 3;
        }
        return switch (value) {
            case 0, 1, 2, 3 -> value;
            default -> 3;
        };
    }

    private Integer normalizeHouse(Integer value) {
        if (value == null) {
            return 2;
        }
        return value == 1 ? 1 : 2;
    }

    private Integer normalizeOverdue(Integer value) {
        if (value == null) {
            return 1;
        }
        return value == 2 ? 2 : 1;
    }

    private Integer normalizeVehicle(Integer value) {
        if (value == null) {
            return 2;
        }
        return value == 1 ? 1 : 2;
    }

    private Integer normalizeLoanAmount(Integer value) {
        if (value == null) {
            return 2;
        }
        if (value >= 1 && value <= 4) {
            return value;
        }
        if (value <= 30000) {
            return 1;
        }
        if (value <= 50000) {
            return 2;
        }
        if (value <= 100000) {
            return 3;
        }
        return 4;
    }

    private boolean isSuccess(JSONObject resp) {
        if (resp == null) {
            return false;
        }
        Integer code = resp.getInteger("code");
        if (code != null && (code == 0 || code == 1 || code == 200)) {
            return true;
        }
        return Boolean.TRUE.equals(resp.getBoolean("success"));
    }

    private JSONObject resolveResultData(JSONObject resp) {
        if (resp == null) {
            return null;
        }
        JSONObject data = resp.getJSONObject("data");
        return data != null ? data : resp;
    }

    private BigDecimal resolvePrice(JSONObject data, JSONObject resp) {
        if (data != null && data.getBigDecimal("price") != null) {
            return data.getBigDecimal("price");
        }
        if (resp != null && resp.getBigDecimal("price") != null) {
            return resp.getBigDecimal("price");
        }
        return BigDecimal.ZERO;
    }

    private java.util.List<java.util.Map<String, Object>> resolveProtocolList(JSONObject data, JSONObject resp) {
        if (data != null && data.getJSONArray("protocolList") != null) {
            return data.getJSONArray("protocolList").toJavaList(JSONObject.class).stream()
                    .map(item -> new java.util.LinkedHashMap<String, Object>(item))
                    .collect(java.util.stream.Collectors.toList());
        }
        if (resp != null && resp.getJSONArray("protocolList") != null) {
            return resp.getJSONArray("protocolList").toJavaList(JSONObject.class).stream()
                    .map(item -> new java.util.LinkedHashMap<String, Object>(item))
                    .collect(java.util.stream.Collectors.toList());
        }
        return null;
    }

    private String resolveText(JSONObject data, JSONObject resp, String... keys) {
        for (String key : keys) {
            if (data != null && StringUtils.hasText(data.getString(key))) {
                return data.getString(key);
            }
            if (resp != null && StringUtils.hasText(resp.getString(key))) {
                return resp.getString(key);
            }
        }
        return null;
    }
}
