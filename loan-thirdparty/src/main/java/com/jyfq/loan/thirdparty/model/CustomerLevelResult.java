package com.jyfq.loan.thirdparty.model;

import lombok.Data;

/**
 * Downstream returned customer star level.
 */
@Data
public class CustomerLevelResult {

    private boolean success;
    private String customerLevel;
    private String errorMsg;

    public static CustomerLevelResult success(String customerLevel) {
        CustomerLevelResult result = new CustomerLevelResult();
        result.setSuccess(true);
        result.setCustomerLevel(customerLevel);
        return result;
    }

    public static CustomerLevelResult unsupported(String errorMsg) {
        CustomerLevelResult result = new CustomerLevelResult();
        result.setSuccess(false);
        result.setErrorMsg(errorMsg);
        return result;
    }

    public static CustomerLevelResult failure(String errorMsg) {
        CustomerLevelResult result = new CustomerLevelResult();
        result.setSuccess(false);
        result.setErrorMsg(errorMsg);
        return result;
    }
}
