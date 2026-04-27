package com.jyfq.loan.app.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.common.result.ResultCode;
import com.jyfq.loan.common.util.AesUtil;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.enums.NotifyStatus;
import com.jyfq.loan.service.NotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

/**
 * Downstream notify callback endpoint.
 */
@Slf4j
@Tag(name = "机构回调")
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;
    private final InstitutionMapper institutionMapper;

    @Operation(summary = "接收机构回调")
    @PostMapping("/{instCode}")
    public R<?> handleNotify(@PathVariable String instCode,
                             @RequestBody String rawBody) {
        Institution institution = institutionMapper.selectOne(new LambdaQueryWrapper<Institution>()
                .eq(Institution::getInstCode, instCode));
        String plainBody = decryptNotifyBody(institution, rawBody);
        log.info("[NOTIFY] receive notify instCode={} rawBody={} plainBody={}", instCode, rawBody, plainBody);

        Map<String, Object> body;
        try {
            body = JSON.parseObject(plainBody, Map.class);
        } catch (Exception ex) {
            log.error("[NOTIFY] parse notify body failed, instCode={}, body={}", instCode, plainBody, ex);
            return R.fail(ResultCode.FAIL.getCode(), "invalid notify body");
        }

        String orderNo = readString(body, "orderNo", "order_no", "bizOrderNo", "biz_order_no");
        String notifyNo = readString(body, "notifyNo", "notify_no", "serialNo", "serial_no", "requestId", "request_id");
        NotifyStatus status = resolveStatus(body);

        if (!StringUtils.hasText(orderNo)) {
            return R.fail(ResultCode.FAIL.getCode(), "missing orderNo");
        }
        if (!StringUtils.hasText(notifyNo)) {
            return R.fail(ResultCode.FAIL.getCode(), "missing notifyNo");
        }
        if (status == null) {
            return R.fail(ResultCode.FAIL.getCode(), "unsupported notify status");
        }

        notifyService.handleNotify(instCode, notifyNo, orderNo, status, plainBody);
        return R.ok();
    }

    private String decryptNotifyBody(Institution institution, String rawBody) {
        if (!StringUtils.hasText(rawBody) || institution == null) {
            return rawBody;
        }
        String encryptType = normalizeEncryptType(institution.getNotifyEncryptType());
        if ("PLAIN".equals(encryptType) || !StringUtils.hasText(institution.getAppKey())) {
            return rawBody;
        }

        String cipherText = extractCipherText(rawBody);
        try {
            return AesUtil.decryptByTransformation(
                    cipherText,
                    institution.getAppKey(),
                    buildTransformation(encryptType, institution.getNotifyCipherMode(), institution.getNotifyPaddingMode()),
                    resolveIv(institution.getNotifyIvValue(), institution.getAppKey()));
        } catch (RuntimeException ex) {
            log.warn("[NOTIFY] decrypt failed, fallback raw body, instCode={}", institution.getInstCode(), ex);
            return rawBody;
        }
    }

    private String extractCipherText(String rawBody) {
        String trimmed = rawBody.trim();
        if (trimmed.startsWith("{")) {
            JSONObject jsonObject = JSON.parseObject(trimmed);
            if (jsonObject != null && StringUtils.hasText(jsonObject.getString("data"))) {
                return jsonObject.getString("data").trim();
            }
        }
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return JSON.parseObject(trimmed, String.class);
        }
        return trimmed;
    }

    private String normalizeEncryptType(String encryptType) {
        return StringUtils.hasText(encryptType) ? encryptType.trim().toUpperCase(Locale.ROOT) : "PLAIN";
    }

    private String buildTransformation(String encryptType, String cipherMode, String paddingMode) {
        String algorithm = switch (encryptType) {
            case "AES_ECB", "AES_CBC", "CBC", "ECB" -> "AES";
            default -> encryptType;
        };
        String mode = StringUtils.hasText(cipherMode)
                ? cipherMode.trim().toUpperCase(Locale.ROOT)
                : ("AES_ECB".equals(encryptType) || "ECB".equals(encryptType) ? "ECB" : "CBC");
        String padding = StringUtils.hasText(paddingMode) ? paddingMode.trim() : "PKCS5Padding";
        return algorithm + "/" + mode + "/" + padding;
    }

    private String resolveIv(String ivValue, String appKey) {
        if (StringUtils.hasText(ivValue)) {
            return ivValue.trim();
        }
        return appKey.substring(0, Math.min(appKey.length(), 16));
    }

    private NotifyStatus resolveStatus(Map<String, Object> body) {
        Object rawStatus = firstNonNull(body,
                "status", "notifyStatus", "notify_status", "orderStatus", "order_status", "code");
        if (rawStatus == null) {
            return null;
        }

        if (rawStatus instanceof Number number) {
            try {
                return NotifyStatus.of(number.intValue());
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        String normalized = String.valueOf(rawStatus).trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "1", "APPROVE", "APPROVED", "PASS", "SUCCESS" -> NotifyStatus.APPROVE;
            case "2", "REJECT", "REJECTED", "REFUSE", "FAIL" -> NotifyStatus.REJECT;
            case "3", "LOAN", "LOANED", "DISBURSE", "DISBURSED" -> NotifyStatus.LOAN;
            case "4", "LOAN_FAIL", "LOANFAILED", "DISBURSE_FAIL", "DISBURSE_FAILED" -> NotifyStatus.LOAN_FAIL;
            default -> null;
        };
    }

    private String readString(Map<String, Object> body, String... keys) {
        Object value = firstNonNull(body, keys);
        return value == null ? null : String.valueOf(value).trim();
    }

    private Object firstNonNull(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return value;
            }
        }
        return null;
    }
}
