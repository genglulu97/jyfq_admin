package com.jyfq.loan.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 并行撞库专用线程池配置
 */
@Configuration
public class CollisionThreadPoolConfig {

    @Bean("collisionExecutor")
    public Executor collisionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：根据 CPU 核数和并发量调整
        executor.setCorePoolSize(10);
        // 最大线程数
        executor.setMaxPoolSize(50);
        // 队列容量
        executor.setQueueCapacity(100);
        // 线程前缀
        executor.setThreadNamePrefix("collision-task-");
        // 拒绝策略：由调用线程处理（退化为串行，保证不丢单）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
