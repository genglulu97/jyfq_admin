package com.jyfq.loan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.mapper.DeductionRecordMapper;
import com.jyfq.loan.model.entity.ApplyOrder;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.entity.DeductionRecord;
import com.jyfq.loan.model.enums.NotifyStatus;
import com.jyfq.loan.service.DeductionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}
