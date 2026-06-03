package com.jyfq.loan.admin.controller;

import com.jyfq.loan.admin.support.CrmCurrentUserResolver;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.vo.CrmDashboardVO;
import com.jyfq.loan.model.vo.CrmTeamSummaryVO;
import com.jyfq.loan.service.CrmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CRM Dashboard")
@RestController
@RequestMapping("/admin/crm/dashboard")
@RequiredArgsConstructor
public class CrmDashboardController {

    private final CrmService crmService;
    private final CrmCurrentUserResolver currentUserResolver;

    @Operation(summary = "dashboard overview")
    @GetMapping("/overview")
    public R<CrmDashboardVO> overview() {
        return R.ok(crmService.dashboard(currentUserResolver.resolve()));
    }

    @Operation(summary = "team summary")
    @GetMapping("/team-summary")
    public R<CrmTeamSummaryVO> teamSummary(@RequestParam(required = false) Long teamId) {
        return R.ok(crmService.teamSummary(teamId, currentUserResolver.resolve()));
    }
}
