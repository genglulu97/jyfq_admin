package com.jyfq.loan.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步事件监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyUpstreamListener {

    /**
     * 异步通知上游渠道
     */
    @Async
    @EventListener
    public void onNotifyUpstream(NotifyUpstreamEvent event) {
        log.info("[EVENT] 异步通知上游 orderNo={} status={}", event.getOrderNo(), event.getStatus());
        // TODO: 调用渠道方回调接口
    }
}
