package com.jyfq.loan.thirdparty.model;

import lombok.Data;

/**
 * Request for downstream customer star-level query.
 */
@Data
public class CustomerLevelRequest {

    private String phoneMd5;
    private String phone;
    private String name;
    private String idCard;
    private String localOrderNo;
    private String thirdOrderNo;
    private Long productId;
}
