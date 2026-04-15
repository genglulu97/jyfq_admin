package com.jyfq.loan.job.handler;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 对账定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationJobHandler {

    /**
     * 日终对账：核对推单记录与机构方回调数据
     */
    @XxlJob("reconciliationJob")
    public void reconciliationJob() {
        log.info("[JOB] 对账任务开始执行...");
        // TODO: 实现对账逻辑
        log.info("[JOB] 对账任务执行完成");
    }
}
