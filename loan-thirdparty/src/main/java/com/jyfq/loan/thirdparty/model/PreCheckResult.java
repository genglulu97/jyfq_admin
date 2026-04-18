package com.jyfq.loan.thirdparty.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 预授信结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreCheckResult {

    /** 是否通过 */
    private boolean pass;

    /** 实时出价/单价 */
    private BigDecimal price;

    /** 第三方流水号/UUID */
    private String uuid;

    /** 下游订单号 */
    private String orderId;

    /** 下游产品图标 */
    private String productLogo;

    /** 下游产品名称 */
    private String productName;

    /** 下游公司名称 */
    private String companyName;

    /** 下游协议列表 */
    private List<Map<String, Object>> protocolList;

    /** 产品ID */
    private Long productId;

    /** 机构ID */
    private Long instId;

    /** 机构编码 */
    private String instCode;

    /** 拒绝原因（未通过时） */
    private String rejectReason;
}
