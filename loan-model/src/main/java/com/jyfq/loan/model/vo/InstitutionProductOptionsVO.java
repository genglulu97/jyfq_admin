package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Options payload for institution product page.
 */
@Data
public class InstitutionProductOptionsVO implements Serializable {

    private List<OptionVO> merchants;
    private List<OptionVO> channels;
    private List<OptionVO> cities;
    private List<OptionVO> statusOptions;
    private List<OptionVO> binaryOptions;
    private List<OptionVO> professionOptions;
    private List<OptionVO> overdueOptions;
    private List<OptionVO> zhimaOptions;
    private List<OptionVO> weekOptions;
}
