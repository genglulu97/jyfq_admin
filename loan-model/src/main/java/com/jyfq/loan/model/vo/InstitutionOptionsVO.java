package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Options payload for institution create/update page.
 */
@Data
public class InstitutionOptionsVO implements Serializable {

    private List<OptionVO> merchantTypeOptions;
    private List<OptionVO> channelTypeOptions;
}
