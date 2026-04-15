package com.jyfq.loan.thirdparty.model;

import lombok.Data;

/**
 * 预授信请求
 */
@Data
public class PreCheckRequest {

    /** 手机号 */
    private String phone;

    /** 身份证号 */
    private String idCard;

    /** 姓名 */
    private String name;

    /** 手机号MD5 */
    private String phoneMd5;

    /** 身份证MD5 */
    private String idCardMd5;

    /** 年龄 */
    private Integer age;

    /** 城市编码 */
    private String cityCode;

    /** 借款金额 */
    private Integer amount;

    /** 产品ID */
    private Long productId;

    /** 机构编码 */
    private String instCode;
}
