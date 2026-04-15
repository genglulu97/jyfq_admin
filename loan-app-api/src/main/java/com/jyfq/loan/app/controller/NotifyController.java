package com.jyfq.loan.app.controller;

import com.alibaba.fastjson2.JSON;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.common.result.ResultCode;
import com.jyfq.loan.model.enums.NotifyStatus;
import com.jyfq.loan.service.NotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 鏈烘瀯鍥炶皟閫氱煡鎺ュ彛
 */
@Slf4j
@Tag(name = "鍥炶皟閫氱煡")
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    @Operation(summary = "鎺ユ敹鏈烘瀯鍥炶皟閫氱煡")
    @PostMapping("/{instCode}")
    public R<?> handleNotify(@PathVariable String instCode,
                             @RequestBody Map<String, Object> body) {
        log.info("[NOTIFY] 鏀跺埌鏈烘瀯鍥炶皟 instCode={} body={}", instCode, body);

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

        notifyService.handleNotify(instCode, notifyNo, orderNo, status, JSON.toJSONString(body));
        return R.ok();
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

        String normalized = String.valueOf(rawStatus).trim().toUpperCase();
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
