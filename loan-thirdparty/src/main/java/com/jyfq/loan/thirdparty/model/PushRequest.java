package com.jyfq.loan.thirdparty.model;

import com.jyfq.loan.model.dto.StandardApplyData;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 推单请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushRequest {

    /** 业务单号 */
    private String orderNo;

    /** 全链路 TraceID */
    private String traceId;

    /** 标准化进件数据 (包含所有明文信息，由基类负责加密发送) */
    private StandardApplyData standardData;

    /** 产品ID */
    private Long productId;

    /** 机构编码 */
    private String instCode;

    /** 回调通知地址 */
    private String notifyUrl;

    /** 内部订单ID */
    private Long orderId;
}
