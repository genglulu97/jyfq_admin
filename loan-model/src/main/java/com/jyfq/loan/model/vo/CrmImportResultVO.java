package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class CrmImportResultVO implements Serializable {
    private int successCount;
    private int duplicateCount;
    private int failCount;
    private List<String> errors = new ArrayList<>();
}
