package com.jyfq.loan.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class CrmDashboardVO implements Serializable {
    private Long todayNewCustomerCount;
    private Long todayFollowedCustomerCount;
    private Long todayPendingFollowCount;
    private Long todayCallCount;
    private Long intentionCustomerCount;
    private Long highQualityCustomerCount;
    private Long unfollowedCustomerCount;
    private Long publicPoolCustomerCount;
    private Long myCustomerCount;
    private Long teamCustomerCount;
    private List<CrmCustomerVO> todayPendingFollowCustomers;
    private List<CrmFollowRecordVO> recentFollowRecords;
    private List<Map<String, Object>> customerStatusDistribution;
    private List<Map<String, Object>> qualityStarDistribution;
    private List<Map<String, Object>> sourceDistribution;
    private List<Map<String, Object>> employeeFollowRanking;
}
