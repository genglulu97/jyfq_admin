package com.jyfq.loan.model.vo;

import com.jyfq.loan.model.common.QualificationRules;
import com.jyfq.loan.model.dto.WorkingHourDTO;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Institution product routing detail.
 */
@Data
public class InstitutionProductDetailVO implements Serializable {

    private Long id;
    private Long instId;
    private String merchantName;
    private String merchantAlias;
    private Integer status;
    private String statusDesc;
    private Integer minAge;
    private Integer maxAge;
    private List<String> cityCodes;
    private List<String> cityNames;
    private List<String> excludedCityCodes;
    private List<String> excludedCityNames;
    private BigDecimal unitPrice;
    private BigDecimal priceRatio;
    private Integer cityQuota;
    private Integer weight;
    private List<String> specifiedChannelCodes;
    private List<String> specifiedChannelNames;
    private List<String> excludedChannelCodes;
    private List<String> excludedChannelNames;
    private List<WorkingHourDTO> workingHours;
    private Integer minAmount;
    private Integer maxAmount;
    private String loanAmountRangeDesc;
    private QualificationRules qualificationRules;
    private Integer providentFund;
    private String providentFundDesc;
    private Integer socialSecurity;
    private String socialSecurityDesc;
    private String zhimaLevel;
    private Integer commercialInsurance;
    private String commercialInsuranceDesc;
    private Integer profession;
    private String professionDesc;
    private Integer house;
    private String houseDesc;
    private Integer vehicle;
    private String vehicleDesc;
    private Integer overdue;
    private String overdueDesc;
    private String householdRegister;
    private String remark;
    private LocalDateTime createdAt;
    private String createBy;
    private LocalDateTime updatedAt;
    private String updateBy;
}
