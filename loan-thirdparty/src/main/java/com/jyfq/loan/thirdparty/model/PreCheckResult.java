package com.jyfq.loan.thirdparty.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

    /** 产品ID */
    private Long productId;

    /** 机构ID */
    private Long instId;

    /** 机构编码 */
    private String instCode;

    /** 拒绝原因（未通过时） */
    private String rejectReason;
}
