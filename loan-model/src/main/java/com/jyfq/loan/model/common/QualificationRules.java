package com.jyfq.loan.model.common;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serializable;

/**
 * Qualification rules configured by AND/OR groups.
 */
@Data
public class QualificationRules implements Serializable {

    @JsonAlias({"mustConditions", "andConditions", "requiredConditions"})
    private QualificationConditionGroup must;

    @JsonAlias({"orConditions", "anyConditions", "optionalConditions"})
    private QualificationConditionGroup any;
}
