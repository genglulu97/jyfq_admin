package com.jyfq.loan.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.jyfq.loan.model.common.QualificationRules;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Institution product create/update request.
 */
@Data
public class InstitutionProductSaveDTO implements Serializable {

    @NotNull(message = "merchant is required")
    private Long instId;

    @NotNull(message = "status is required")
    private Integer status;

    @NotNull(message = "minAge is required")
    private Integer minAge;

    @NotNull(message = "maxAge is required")
    private Integer maxAge;

    private List<String> cityCodes;
    private List<String> cityNames;
    private List<String> excludedCityCodes;
    private List<String> excludedCityNames;

    @NotNull(message = "unitPrice is required")
    @DecimalMin(value = "0", inclusive = true, message = "unitPrice must be >= 0")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0", inclusive = true, message = "priceRatio must be >= 0")
    private BigDecimal priceRatio;

    @NotNull(message = "dailyQuota is required")
    private Integer dailyQuota;

    @NotNull(message = "weight is required")
    private Integer weight;

    private List<String> specifiedChannelCodes;
    private List<String> excludedChannelCodes;
    private List<WorkingHourDTO> workingHours;

    @NotNull(message = "minAmount is required")
    private Integer minAmount;

    @NotNull(message = "maxAmount is required")
    private Integer maxAmount;

    @JsonAlias({"qualificationConfig", "qualificationRule"})
    private QualificationRules qualificationRules;

    private Integer providentFund;
    private Integer socialSecurity;
    private String zhimaLevel;
    private Integer commercialInsurance;
    private Integer profession;
    private Integer house;
    private Integer vehicle;
    private Integer overdue;
    private String householdRegister;
    private String remark;
}
