package com.jyfq.loan.thirdparty.template;

import com.alibaba.fastjson2.JSONObject;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.thirdparty.AbstractInstitutionAdapter;
import com.jyfq.loan.thirdparty.model.PreCheckRequest;
import com.jyfq.loan.thirdparty.model.PreCheckResult;
import com.jyfq.loan.thirdparty.model.PushRequest;
import com.jyfq.loan.thirdparty.model.PushResult;
import com.jyfq.loan.thirdparty.support.DownstreamFieldMappingUtil;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Copy this template into the impl package when integrating a new downstream institution.
 *
 * Usage:
 * 1. Copy this class to com.jyfq.loan.thirdparty.impl
 * 2. Add @Service("yourBeanName") so API config beanName can locate it
 * 3. Replace adapter key / payload field names / enum dictionaries / response parsing
 */
public class ExampleInstitutionAdapterTemplate extends AbstractInstitutionAdapter {

    public static final String EXAMPLE_BEAN_NAME = "replaceWithYourBeanName";

    private static final Map<Integer, String> GENDER_DICT = Map.of(
            1, "M",
            2, "F"
    );

    private static final Map<Integer, String> PROFESSION_DICT = Map.of(
            1, "EMPLOYEE",
            2, "SELF_EMPLOYED",
            3, "BUSINESS_OWNER",
            4, "PUBLIC_SERVANT"
    );

    @Override
    public String getAdapterKey() {
        return "replace_with_adapter_key";
    }

    @Override
    public PreCheckResult preCheck(Institution institution, PreCheckRequest req) {
        if (institution == null || !StringUtils.hasText(institution.getPreCheckUrl())) {
            return PreCheckResult.builder()
                    .pass(false)
                    .rejectReason("preCheckUrl is not configured")
                    .build();
        }

        JSONObject resp = doPost(institution, institution.getPreCheckUrl(), buildPreCheckPayload(institution, req), JSONObject.class);
        return parsePreCheckResponse(institution, resp);
    }

    @Override
    public PushResult push(Institution institution, PushRequest req) {
        if (institution == null || !StringUtils.hasText(institution.getApiPushUrl())) {
            return PushResult.failure("apiPushUrl is not configured");
        }

        JSONObject resp = doPost(institution, institution.getApiPushUrl(), buildPushPayload(institution, req), JSONObject.class);
        return parsePushResponse(institution, resp);
    }

    protected JSONObject buildPreCheckPayload(Institution institution, PreCheckRequest req) {
        JSONObject payload = buildCommonEnvelope(institution);
        payload.put("method", "preCheck");
        payload.put("productId", req.getProductId());
        payload.put("instCode", req.getInstCode());
        payload.put("mobile", req.getPhone());
        payload.put("idCard", req.getIdCard());
        payload.put("name", req.getName());
        payload.put("age", req.getAge());
        payload.put("cityCode", req.getCityCode());
        payload.put("amount", req.getAmount());
        return payload;
    }

    protected JSONObject buildPushPayload(Institution institution, PushRequest req) {
        JSONObject payload = buildCommonEnvelope(institution);
        payload.put("method", "push");
        payload.put("orderNo", req.getOrderNo());
        payload.put("traceId", req.getTraceId());
        payload.put("notifyUrl", req.getNotifyUrl());
        payload.put("productId", req.getProductId());
        payload.put("instCode", req.getInstCode());
        appendMappedApplyFields(payload, req.getStandardData());
        return payload;
    }

    protected void appendMappedApplyFields(JSONObject payload, StandardApplyData data) {
        if (payload == null || data == null) {
            return;
        }

        payload.put("mobile", data.getPhone());
        payload.put("name", data.getName());
        payload.put("idCard", data.getIdCard());
        payload.put("cityCode", data.getCityCode());
        payload.put("workCity", data.getWorkCity());
        payload.put("loanAmount", data.getLoanAmount());

        // Example enum mappings. Replace these dictionaries and field names per downstream doc.
        DownstreamFieldMappingUtil.putIfNotNull(payload, "genderCode",
                DownstreamFieldMappingUtil.mapFromDict(data.getGender(), GENDER_DICT, null));
        DownstreamFieldMappingUtil.putIfNotNull(payload, "professionCode",
                DownstreamFieldMappingUtil.mapFromDict(data.getProfession(), PROFESSION_DICT, null));
        DownstreamFieldMappingUtil.putIfNotNull(payload, "houseFlag",
                DownstreamFieldMappingUtil.binaryAssetText(data.getHouse()));
        DownstreamFieldMappingUtil.putIfNotNull(payload, "carFlag",
                DownstreamFieldMappingUtil.binaryAssetText(data.getVehicle()));
        DownstreamFieldMappingUtil.putIfNotNull(payload, "socialSecurityLevel",
                DownstreamFieldMappingUtil.durationText(data.getSocialSecurity()));
        DownstreamFieldMappingUtil.putIfNotNull(payload, "providentFundLevel",
                DownstreamFieldMappingUtil.durationText(data.getProvidentFund()));
        DownstreamFieldMappingUtil.putIfNotNull(payload, "insuranceLevel",
                DownstreamFieldMappingUtil.durationText(data.getCommercialInsurance()));
        DownstreamFieldMappingUtil.putIfNotNull(payload, "overdueLabel",
                DownstreamFieldMappingUtil.overdueText(data.getOverdue()));
        DownstreamFieldMappingUtil.putIfNotNull(payload, "zhimaBucket",
                DownstreamFieldMappingUtil.zhimaBucket(data.getZhima()));
    }

    protected JSONObject buildCommonEnvelope(Institution institution) {
        JSONObject payload = new JSONObject();
        if (institution != null) {
            payload.put("instCode", institution.getInstCode());
            payload.put("businessCode", institution.getBusinessCode());
            payload.put("apiKey", institution.getAppKey());
            payload.put("encryptType", institution.getEncryptType());
        }
        return payload;
    }

    protected PreCheckResult parsePreCheckResponse(Institution institution, JSONObject resp) {
        if (isSuccess(resp)) {
            JSONObject data = resolveResultData(resp);
            return PreCheckResult.builder()
                    .pass(true)
                    .instCode(institution == null ? null : institution.getInstCode())
                    .price(resolvePrice(data, resp))
                    .uuid(resolveText(data, resp, "uuid", "requestId", "traceId"))
                    .build();
        }
        return PreCheckResult.builder()
                .pass(false)
                .instCode(institution == null ? null : institution.getInstCode())
                .rejectReason(resp == null ? "preCheck failed" : resolveText(resolveResultData(resp), resp, "msg", "message", "errorMsg"))
                .build();
    }

    protected PushResult parsePushResponse(Institution institution, JSONObject resp) {
        if (isSuccess(resp)) {
            PushResult result = PushResult.success(resolveText(resolveResultData(resp), resp, "msg", "message", "resultMsg"));
            result.setInstCode(institution == null ? null : institution.getInstCode());
            result.setThirdOrderNo(resolveText(resolveResultData(resp), resp, "thirdOrderNo", "orderNo", "applyNo"));
            return result;
        }
        PushResult result = PushResult.failure(resp == null ? "push failed" : resolveText(resolveResultData(resp), resp, "msg", "message", "errorMsg"));
        result.setInstCode(institution == null ? null : institution.getInstCode());
        return result;
    }

    protected boolean isSuccess(JSONObject resp) {
        if (resp == null) {
            return false;
        }
        Integer code = resp.getInteger("code");
        if (code != null && (code == 0 || code == 1 || code == 200)) {
            return true;
        }
        return Boolean.TRUE.equals(resp.getBoolean("success"));
    }

    protected JSONObject resolveResultData(JSONObject resp) {
        if (resp == null) {
            return null;
        }
        JSONObject data = resp.getJSONObject("data");
        return data != null ? data : resp;
    }

    protected BigDecimal resolvePrice(JSONObject data, JSONObject resp) {
        if (data != null && data.getBigDecimal("price") != null) {
            return data.getBigDecimal("price");
        }
        if (resp != null && resp.getBigDecimal("price") != null) {
            return resp.getBigDecimal("price");
        }
        return BigDecimal.ZERO;
    }

    protected String resolveText(JSONObject data, JSONObject resp, String... keys) {
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
