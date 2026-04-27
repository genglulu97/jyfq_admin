package com.jyfq.loan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.mapper.DeductionRecordMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.model.entity.ApplyOrder;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.entity.DeductionRecord;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.enums.NotifyStatus;
import com.jyfq.loan.service.DeductionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * Deduction service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeductionServiceImpl implements DeductionService {

    private final DeductionRecordMapper deductionMapper;
    private final ChannelMapper channelMapper;
    private final ApplyOrderMapper applyOrderMapper;
    private final InstitutionMapper institutionMapper;

    @Override
    public void createDeduction(String orderNo, String instCode, NotifyStatus status) {
        log.info("[DEDUCT] create deduction, orderNo={}, instCode={}, status={}", orderNo, instCode, status);

        ApplyOrder order = applyOrderMapper.selectOne(new LambdaQueryWrapper<ApplyOrder>()
                .eq(ApplyOrder::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null || order.getChannelId() == null) {
            log.warn("[DEDUCT] missing order/channel snapshot, skip deduction, orderNo={}", orderNo);
            return;
        }

        Channel channel = channelMapper.selectById(order.getChannelId());
        if (channel == null || channel.getFeeRate() == null) {
            log.warn("[DEDUCT] missing channel fee config, skip deduction, orderNo={}", orderNo);
            return;
        }

        DeductionRecord record = new DeductionRecord();
        record.setOrderNo(orderNo);
        record.setChannelId(order.getChannelId());
        record.setInstId(order.getInstId());
        record.setInstCode(instCode);
        record.setProductId(order.getProductId());
        record.setDeductType(status == NotifyStatus.APPROVE ? 1 : 2);
        record.setAmount(channel.getFeeRate());
        record.setStatus(1);
        record.setRemark("auto deduction");
        deductionMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPushSuccessDeduction(String orderNo) {
        log.info("[DEDUCT] create push-success deduction, orderNo={}", orderNo);

        ApplyOrder order = applyOrderMapper.selectOne(new LambdaQueryWrapper<ApplyOrder>()
                .eq(ApplyOrder::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null || order.getInstId() == null || order.getProductId() == null) {
            log.warn("[DEDUCT] missing order/institution snapshot, skip push-success deduction, orderNo={}", orderNo);
            return;
        }
        if (order.getSettlementPrice() == null || order.getSettlementPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[DEDUCT] missing settlement price, skip push-success deduction, orderNo={}", orderNo);
            return;
        }

        long existingCount = deductionMapper.selectCount(new LambdaQueryWrapper<DeductionRecord>()
                .eq(DeductionRecord::getOrderNo, orderNo)
                .eq(DeductionRecord::getInstId, order.getInstId())
                .eq(DeductionRecord::getProductId, order.getProductId())
                .eq(DeductionRecord::getDeductType, 3)
                .eq(DeductionRecord::getStatus, 1));
        if (existingCount > 0) {
            log.info("[DEDUCT] push-success deduction already exists, orderNo={}", orderNo);
            return;
        }

        Institution institution = institutionMapper.selectById(order.getInstId());
        if (institution == null) {
            log.warn("[DEDUCT] institution not found, skip push-success deduction, instId={}, orderNo={}", order.getInstId(), orderNo);
            return;
        }

        BigDecimal beforeBalance = institution.getAccountBalance() == null ? BigDecimal.ZERO : institution.getAccountBalance();
        BigDecimal afterBalance = beforeBalance.subtract(order.getSettlementPrice());

        deductionMapper.insert(buildPushSuccessDeductionRecord(order, institution.getInstCode(), order.getSettlementPrice()));
        institutionMapper.update(null, new LambdaUpdateWrapper<Institution>()
                .eq(Institution::getId, institution.getId())
                .set(Institution::getAccountBalance, afterBalance));

        log.info("[DEDUCT] push-success deduction completed, orderNo={}, instCode={}, amount={}, beforeBalance={}, afterBalance={}",
                orderNo, institution.getInstCode(), order.getSettlementPrice(), beforeBalance, afterBalance);
    }

    private DeductionRecord buildPushSuccessDeductionRecord(ApplyOrder order, String instCode, BigDecimal amount) {
        DeductionRecord record = new DeductionRecord();
        record.setOrderNo(order.getOrderNo());
        record.setChannelId(order.getChannelId());
        record.setInstId(order.getInstId());
        record.setInstCode(StringUtils.hasText(instCode) ? instCode : null);
        record.setProductId(order.getProductId());
        record.setDeductType(3);
        record.setAmount(amount);
        record.setStatus(1);
        record.setRemark("apply success deduction");
        return record;
    }
}
