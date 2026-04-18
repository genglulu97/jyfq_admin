package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Options payload for institution API config page.
 */
@Data
public class InstitutionApiConfigOptionsVO implements Serializable {

    private List<OptionVO> beanOptions;
    private List<OptionVO> encryptTypeOptions;
    private List<OptionVO> notifyEncryptTypeOptions;
}
