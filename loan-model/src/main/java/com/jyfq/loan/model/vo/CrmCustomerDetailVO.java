package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CrmCustomerDetailVO extends CrmCustomerVO implements Serializable {
    private String occupation;
    private BigDecimal monthlyIncome;
    private Integer hasSocialSecurity;
    private Integer hasHousingFund;
    private Integer hasHouse;
    private Integer hasCar;
    private Integer sesameScore;
    private String creditCardStatus;
    private String expectedTerm;
    private Integer isValid;
    private Integer wechatAdded;
    private Integer needRecall;
    private Integer isDeal;
    private Integer isRejected;
    private String wagePaymentType;
    private String socialSecurityStatus;
    private String housingFundStatus;
    private String houseStatus;
    private String carStatus;
    private String insuranceStatus;
    private String creditStatus;
    private Integer hasOverdue;
    private BigDecimal currentDebt;
    private Integer hasCreditCard;
    private Integer hasOnlineLoan;
    private BigDecimal acceptableRate;
    private Integer urgentMoney;
    private String remark;
    private List<CrmFollowRecordVO> followRecords;
}
