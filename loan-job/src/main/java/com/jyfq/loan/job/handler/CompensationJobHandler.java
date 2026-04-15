package com.jyfq.loan.job.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.model.entity.ApplyOrder;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 状态补偿定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationJobHandler {

    private final ApplyOrderMapper applyOrderMapper;

    /**
     * 状态补偿：处理超时未回调的推单记录
     */
    @XxlJob("statusCompensationJob")
    public void statusCompensationJob() {
        log.info("[JOB] 状态补偿任务开始执行...");
        
        // 1. 查询 5 分钟前仍处于 "1(推单中)" 状态的订单
        LocalDateTime triggerTime = LocalDateTime.now().minusMinutes(5);
        List<ApplyOrder> stuckOrders = applyOrderMapper.selectList(new LambdaQueryWrapper<ApplyOrder>()
                .eq(ApplyOrder::getOrderStatus, 1)
                .lt(ApplyOrder::getCreatedAt, triggerTime));

        if (stuckOrders.isEmpty()) {
            log.info("[JOB] 无需补偿的超时订单");
            return;
        }

        log.info("[JOB] 发现 {} 个超时订单需同步状态", stuckOrders.size());

        // 2. 遍历并同步状态
        for (ApplyOrder order : stuckOrders) {
            try {
                processStuckOrder(order);
            } catch (Exception e) {
                log.error("[JOB] 补偿订单异常: orderNo={}", order.getOrderNo(), e);
            }
        }

        log.info("[JOB] 状态补偿任务执行完成");
    }

    private void processStuckOrder(ApplyOrder order) {
        // TODO: 调用各机构适配器的查询接口
        log.info("[JOB] 正在同步订单状态: orderNo={}", order.getOrderNo());
    }
}
