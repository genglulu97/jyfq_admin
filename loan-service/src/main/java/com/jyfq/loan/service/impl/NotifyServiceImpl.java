package com.jyfq.loan.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jyfq.loan.common.util.AuditOperatorUtil;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.mapper.NotifyRecordMapper;
import com.jyfq.loan.model.entity.ApplyOrder;
import com.jyfq.loan.model.entity.NotifyRecord;
import com.jyfq.loan.model.enums.NotifyStatus;
import com.jyfq.loan.service.DeductionService;
import com.jyfq.loan.service.NotifyService;
import com.jyfq.loan.service.event.NotifyUpstreamEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Notify service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final NotifyRecordMapper notifyRecordMapper;
    private final ApplyOrderMapper applyOrderMapper;
    private final DeductionService deductionService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleNotify(String instCode, String notifyNo, String orderNo, NotifyStatus status, String rawBody) {
        log.info("[NOTIFY] receive notify, instCode={}, notifyNo={}, orderNo={}, status={}",
                instCode, notifyNo, orderNo, status);

        if (notifyRecordMapper.existsByInstAndNo(instCode, notifyNo)) {
            log.warn("[NOTIFY] duplicated notify ignored, notifyNo={}", notifyNo);
            return;
        }

        NotifyRecord record = new NotifyRecord();
        record.setInstCode(instCode);
        record.setNotifyNo(notifyNo);
        record.setOrderNo(orderNo);
        record.setStatus(status.name());
        record.setRawBody(rawBody);
        record.setIsProcessed(1);
        record.setProcessedAt(LocalDateTime.now());
        notifyRecordMapper.insert(record);

        int orderStatus = mapNotifyStatusToOrderStatus(status);
        applyOrderMapper.update(null, new LambdaUpdateWrapper<ApplyOrder>()
                .eq(ApplyOrder::getOrderNo, orderNo)
                .set(ApplyOrder::getOrderStatus, orderStatus)
                .set(ApplyOrder::getUpdateBy, AuditOperatorUtil.currentOperator())
                .set(status == NotifyStatus.REJECT || status == NotifyStatus.LOAN_FAIL,
                        ApplyOrder::getRejectReason, status.name()));

        if (status == NotifyStatus.APPROVE || status == NotifyStatus.LOAN) {
            deductionService.createDeduction(orderNo, instCode, status);
        }

        eventPublisher.publishEvent(new NotifyUpstreamEvent(this, orderNo, status));
    }

    private int mapNotifyStatusToOrderStatus(NotifyStatus status) {
        return switch (status) {
            case APPROVE -> 2;
            case REJECT -> 9;
            case LOAN -> 3;
            case LOAN_FAIL -> 9;
            default -> 1;
        };
    }
}
