package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class CrmTeamSummaryVO implements Serializable {
    private Long teamId;
    private Long customerCount;
    private List<CrmEmployeeProfileVO> employees;
    private List<Map<String, Object>> statusDistribution;
    private List<Map<String, Object>> intentionDistribution;
    private List<Map<String, Object>> qualityStarDistribution;
}
