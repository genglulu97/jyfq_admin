package com.jyfq.loan.thirdparty.impl;

import com.alibaba.fastjson2.JSONObject;
import com.jyfq.loan.common.util.AesUtil;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.thirdparty.AbstractInstitutionAdapter;
import com.jyfq.loan.thirdparty.model.PreCheckRequest;
import com.jyfq.loan.thirdparty.model.PreCheckResult;
import com.jyfq.loan.thirdparty.model.PushRequest;
import com.jyfq.loan.thirdparty.model.PushResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 德立金 downstream adapter.
 * Protocol: top-level JSON plain text, only the "data" field is AES/ECB/PKCS5Padding encrypted.
 */
@Service("deljApiPushService")
public class DeLjApiAdapter extends AbstractInstitutionAdapter {

    private static final Map<Integer, Integer> LOAN_TIME_MAP = Map.of(
            6, 2,
            12, 3,
            24, 4,
            36, 5
    );

    private static final Map<Integer, Integer> PROFESSION_MAP = Map.of(
            1, 1,
            2, 2,
            3, 3,
            4, 4
    );

    @Override
    public String getAdapterKey() {
        return "delj";
    }

    @Override
    public PreCheckResult preCheck(Institution institution, PreCheckRequest req) {
        if (institution == null || !StringUtils.hasText(institution.getPreCheckUrl())) {
            return PreCheckResult.builder()
                    .pass(false)
                    .rejectReason("preCheckUrl is not configured")
                    .build();
        }

        JSONObject resp = doPlainPost(institution, institution.getPreCheckUrl(), buildPreCheckEnvelope(institution, req), JSONObject.class);
        JSONObject data = resolveResultData(resp);
        if (isSuccess(resp)) {
            return PreCheckResult.builder()
                    .pass(true)
                    .instCode(institution.getInstCode())
                    .uuid(resolveText(data, resp, "orderId", "uuid", "traceId"))
                    .price(resolvePrice(data))
                    .build();
        }
        return PreCheckResult.builder()
                .pass(false)
                .instCode(institution.getInstCode())
                .rejectReason(resolveText(data, resp, "msg", "message", "errorMsg"))
                .build();
    }

    @Override
    public PushResult push(Institution institution, PushRequest req) {
        if (institution == null || !StringUtils.hasText(institution.getApiPushUrl())) {
            return PushResult.failure("apiPushUrl is not configured");
        }

        JSONObject resp = doPlainPost(institution, institution.getApiPushUrl(), buildApplyEnvelope(institution, req), JSONObject.class);
        if (isSuccess(resp)) {
            PushResult result = PushResult.success(resolveText(resolveResultData(resp), resp, "msg", "message"));
            result.setInstCode(institution.getInstCode());
            result.setThirdOrderNo(resolveText(resolveResultData(resp), resp, "orderId", "applyNo", "thirdOrderNo"));
            return result;
        }
        PushResult result = PushResult.failure(resolveText(resolveResultData(resp), resp, "msg", "message", "errorMsg"));
        result.setInstCode(institution.getInstCode());
        return result;
    }

    private JSONObject buildPreCheckEnvelope(Institution institution, PreCheckRequest req) {
        JSONObject plainData = new JSONObject();
        plainData.put("orgId", resolveOrgId(institution));
        plainData.put("phoneMd5", req.getPhoneMd5());
        plainData.put("city", resolveCityName(req.getWorkCity(), req.getCityCode()));
        plainData.put("cityCode", req.getCityCode());
        plainData.put("province", resolveProvinceName(req.getWorkCity(), req.getCityCode()));
        plainData.put("provinceCode", firstSixToProvince(req.getCityCode()));
        plainData.put("age", req.getAge());
        plainData.put("gender", normalizeGender(req.getGender()));
        plainData.put("loanTime", normalizeLoanTime(req.getLoanTime()));
        plainData.put("profession", normalizeProfession(req.getProfession()));
        plainData.put("zhima", normalizeZhima(req.getZhima()));
        plainData.put("providentFund", normalizeProvidentFund(req.getProvidentFund()));
        plainData.put("socialSecurity", normalizeSocialSecurity(req.getSocialSecurity()));
        plainData.put("commericalInsurance", normalizeCommercialInsurance(req.getCommercialInsurance()));
        plainData.put("house", normalizeHouse(req.getHouse()));
        plainData.put("overdue", normalizeOverdue(req.getOverdue()));
        plainData.put("vehicle", normalizeVehicle(req.getVehicle()));
        plainData.put("loanAmount", normalizeLoanAmount(req.getAmount()));
        return wrapEncryptedEnvelope(institution, plainData);
    }

    private JSONObject buildApplyEnvelope(Institution institution, PushRequest req) {
        StandardApplyData data = req.getStandardData();
        JSONObject plainData = new JSONObject();
        plainData.put("orgId", resolveOrgId(institution));
        plainData.put("name", data == null ? null : data.getName());
        plainData.put("phone", data == null ? null : data.getPhone());
        plainData.put("phoneMd5", data == null ? null : data.getPhoneMd5());
        plainData.put("idCard", data == null ? null : data.getIdCard());
        plainData.put("city", data == null ? null : resolveCityName(data.getWorkCity(), data.getCityCode()));
        plainData.put("cityCode", data == null ? null : data.getCityCode());
        plainData.put("province", data == null ? null : resolveProvinceName(data.getWorkCity(), data.getCityCode()));
        plainData.put("provinceCode", data == null ? null : firstSixToProvince(data.getCityCode()));
        plainData.put("age", data == null ? null : data.getAge());
        plainData.put("gender", normalizeGender(data == null ? null : data.getGender()));
        plainData.put("loanTime", normalizeLoanTime(data == null ? null : data.getLoanTime()));
        plainData.put("profession", normalizeProfession(data == null ? null : data.getProfession()));
        plainData.put("zhima", normalizeZhima(data == null ? null : data.getZhima()));
        plainData.put("providentFund", normalizeProvidentFund(data == null ? null : data.getProvidentFund()));
        plainData.put("socialSecurity", normalizeSocialSecurity(data == null ? null : data.getSocialSecurity()));
        plainData.put("commericalInsurance", normalizeCommercialInsurance(data == null ? null : data.getCommercialInsurance()));
        plainData.put("house", normalizeHouse(data == null ? null : data.getHouse()));
        plainData.put("overdue", normalizeOverdue(data == null ? null : data.getOverdue()));
        plainData.put("vehicle", normalizeVehicle(data == null ? null : data.getVehicle()));
        plainData.put("loanAmount", normalizeLoanAmount(data == null ? null : data.getLoanAmount()));
        plainData.put("agreeProtocol", req.getNotifyUrl());
        return wrapEncryptedEnvelope(institution, plainData);
    }

    private JSONObject wrapEncryptedEnvelope(Institution institution, JSONObject plainData) {
        String appKey = institution == null ? null : institution.getAppKey();
        String encryptedData = StringUtils.hasText(appKey)
                ? AesUtil.encryptECB(plainData.toJSONString(), appKey)
                : plainData.toJSONString();
        JSONObject envelope = new JSONObject();
        envelope.put("orgId", resolveOrgId(institution));
        envelope.put("data", encryptedData);
        return envelope;
    }

    private String resolveOrgId(Institution institution) {
        return institution == null ? null : institution.getBusinessCode();
    }

    private boolean isSuccess(JSONObject resp) {
        if (resp == null) {
            return false;
        }
        Integer code = resp.getInteger("code");
        return code != null && code == 0;
    }

    private JSONObject resolveResultData(JSONObject resp) {
        if (resp == null) {
            return null;
        }
        JSONObject data = resp.getJSONObject("data");
        return data != null ? data : resp;
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

    private BigDecimal resolvePrice(JSONObject data) {
        if (data == null || data.getBigDecimal("price") == null) {
            return BigDecimal.ZERO;
        }
        return data.getBigDecimal("price");
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

    private Integer normalizeLoanTime(Integer loanTime) {
        if (loanTime == null) {
            return 3;
        }
        if (LOAN_TIME_MAP.containsKey(loanTime)) {
            return LOAN_TIME_MAP.get(loanTime);
        }
        if (loanTime >= 2 && loanTime <= 5) {
            return loanTime;
        }
        return 3;
    }

    private Integer normalizeProfession(Integer profession) {
        if (profession == null) {
            return 1;
        }
        return PROFESSION_MAP.getOrDefault(profession, 1);
    }

    private Integer normalizeZhima(Integer zhima) {
        if (zhima == null) {
            return 4;
        }
        if (zhima < 600) {
            return 5;
        }
        if (zhima < 650) {
            return 1;
        }
        if (zhima < 700) {
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
            case 0 -> 4;
            default -> 4;
        };
    }

    private Integer normalizeSocialSecurity(Integer value) {
        if (value == null) {
            return 4;
        }
        return switch (value) {
            case 1, 2, 3 -> value;
            case 0 -> 4;
            default -> 4;
        };
    }

    private Integer normalizeCommercialInsurance(Integer value) {
        if (value == null) {
            return 3;
        }
        return switch (value) {
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 2;
            case 0 -> 3;
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

    private Integer normalizeLoanAmount(Integer amount) {
        if (amount == null || amount <= 30000) {
            return 1;
        }
        if (amount <= 50000) {
            return 2;
        }
        if (amount <= 100000) {
            return 3;
        }
        return 4;
    }

    private String resolveCityName(String workCity, String cityCode) {
        if (StringUtils.hasText(workCity)) {
            String[] parts = workCity.split("[,/]");
            return parts[parts.length - 1].trim();
        }
        return cityCode;
    }

    private String resolveProvinceName(String workCity, String cityCode) {
        if (!StringUtils.hasText(workCity)) {
            return cityCode;
        }
        String[] parts = workCity.split("[,/]");
        return parts.length > 0 ? parts[0].trim() : cityCode;
    }

    private String firstSixToProvince(String cityCode) {
        if (!StringUtils.hasText(cityCode) || cityCode.length() < 2) {
            return cityCode;
        }
        if (cityCode.length() >= 4) {
            return cityCode.substring(0, 4) + "00";
        }
        return cityCode;
    }
}
