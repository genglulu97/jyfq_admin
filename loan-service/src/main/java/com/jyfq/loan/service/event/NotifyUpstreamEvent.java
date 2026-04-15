package com.jyfq.loan.service.event;

import com.jyfq.loan.model.enums.NotifyStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 通知上游渠道事件（异步解耦）
 */
@Getter
public class NotifyUpstreamEvent extends ApplicationEvent {

    private final String orderNo;
    private final NotifyStatus status;

    public NotifyUpstreamEvent(Object source, String orderNo, NotifyStatus status) {
        super(source);
        this.orderNo = orderNo;
        this.status = status;
    }
}
