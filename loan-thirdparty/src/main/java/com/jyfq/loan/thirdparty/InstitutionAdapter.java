package com.jyfq.loan.thirdparty;

import com.jyfq.loan.model.entity.Institution;
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
}
