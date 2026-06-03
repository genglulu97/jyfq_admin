package com.jyfq.loan.admin.controller;

import com.jyfq.loan.admin.support.CrmCurrentUserResolver;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.CrmEmployeeProfileQueryDTO;
import com.jyfq.loan.model.dto.CrmEmployeeProfileSaveDTO;
import com.jyfq.loan.model.vo.CrmEmployeeProfileVO;
import com.jyfq.loan.service.CrmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CRM Employee")
@RestController
@RequestMapping("/admin/crm/employee")
@RequiredArgsConstructor
public class CrmEmployeeController {

    private final CrmService crmService;
    private final CrmCurrentUserResolver currentUserResolver;

    @Operation(summary = "employee crm profile list")
    @GetMapping("/list")
    public R<PageResult<CrmEmployeeProfileVO>> list(CrmEmployeeProfileQueryDTO query) {
        return R.ok(crmService.pageEmployeeProfiles(query, currentUserResolver.resolve()));
    }

    @Operation(summary = "save employee crm profile")
    @PostMapping("/save")
    public R<Long> save(@Valid @RequestBody CrmEmployeeProfileSaveDTO request) {
        return R.ok(crmService.saveEmployeeProfile(request, currentUserResolver.resolve()));
    }
}
