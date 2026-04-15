package com.jyfq.loan.model.common;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Qualification options for a single AND/OR rule group.
 */
@Data
public class QualificationConditionGroup implements Serializable {

    @JsonAlias({"professionOptions"})
    private List<String> profession;

    @JsonAlias({"overdueOptions"})
    private List<String> overdue;

    @JsonAlias({"loanAmountOptions", "amountOptions"})
    private List<String> loanAmount;

    @JsonAlias({"loanTimeOptions", "termOptions"})
    private List<String> loanTime;

    @JsonAlias({"zhimaOptions", "zhimaLevel"})
    private List<String> zhima;

    @JsonAlias({"socialSecurityOptions", "socialSecurityMonths"})
    private List<String> socialSecurity;

    @JsonAlias({"providentFundOptions", "providentFundMonths"})
    private List<String> providentFund;

    @JsonAlias({"commercialInsuranceOptions", "insuranceOptions"})
    private List<String> commercialInsurance;

    @JsonAlias({"vehicleOptions"})
    private List<String> vehicle;

    @JsonAlias({"houseOptions"})
    private List<String> house;

    @JsonAlias({"householdRegisterOptions"})
    private List<String> householdRegister;
}
