package com.jyfq.loan.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Simple option item for admin select components.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionVO implements Serializable {

    private String label;
    private String value;
}
