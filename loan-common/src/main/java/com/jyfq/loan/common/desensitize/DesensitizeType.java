package com.jyfq.loan.common.desensitize;

/**
 * 脱敏类型枚举
 */
public enum DesensitizeType {

    /** 手机号：138****8888 */
    PHONE,

    /** 身份证号：110***********0011 */
    ID_CARD,

    /** 姓名：张*三 */
    NAME,

    /** 银行卡号：6222 **** **** 1234 */
    BANK_CARD,

    /** 邮箱：t***@example.com */
    EMAIL,

    /** 地址：北京市朝阳区**** */
    ADDRESS
}
