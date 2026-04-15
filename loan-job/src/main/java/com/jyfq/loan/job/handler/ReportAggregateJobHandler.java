package com.jyfq.loan.job.handler;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 统计聚合定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportAggregateJobHandler {

    /**
     * 小时统计快照生成
     */
    @XxlJob("reportAggregateJob")
    public void reportAggregateJob() {
        log.info("[JOB] 统计聚合任务开始执行...");
        // TODO: 实现小时统计快照聚合逻辑
        log.info("[JOB] 统计聚合任务执行完成");
    }
}
