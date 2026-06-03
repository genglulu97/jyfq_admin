package com.jyfq.loan.thirdparty;

import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.thirdparty.model.PreCheckRequest;
import com.jyfq.loan.thirdparty.model.PreCheckResult;

/**
 * Optional capability for 8-digit masked mobile pre-check.
 */
public interface MobileEightPreCheckAdapter {

    PreCheckResult mobileEightPreCheck(Institution institution, PreCheckRequest req);
}
