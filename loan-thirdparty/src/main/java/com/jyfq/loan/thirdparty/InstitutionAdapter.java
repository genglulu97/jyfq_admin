package com.jyfq.loan.thirdparty;

import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.thirdparty.model.CustomerLevelRequest;
import com.jyfq.loan.thirdparty.model.CustomerLevelResult;
import com.jyfq.loan.thirdparty.model.PreCheckRequest;
import com.jyfq.loan.thirdparty.model.PreCheckResult;
import com.jyfq.loan.thirdparty.model.PushRequest;
import com.jyfq.loan.thirdparty.model.PushResult;

/**
 * Unified downstream institution adapter.
 */
public interface InstitutionAdapter {

    String getAdapterKey();

    PreCheckResult preCheck(Institution institution, PreCheckRequest req);

    PushResult push(Institution institution, PushRequest req);

    /**
     * Reserved for downstream institutions that expose a customer star-level query/update API.
     */
    default CustomerLevelResult queryCustomerLevel(Institution institution, CustomerLevelRequest req) {
        return CustomerLevelResult.unsupported("customer level query not implemented");
    }
}
