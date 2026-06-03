package com.jyfq.loan.admin.controller;

import com.jyfq.loan.admin.support.CrmCurrentUserResolver;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.CrmCustomerAssignDTO;
import com.jyfq.loan.model.dto.CrmPublicPoolQueryDTO;
import com.jyfq.loan.model.vo.CrmCustomerVO;
import com.jyfq.loan.service.CrmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CRM Public Pool")
@RestController
@RequestMapping("/admin/crm/public-pool")
@RequiredArgsConstructor
public class CrmPublicPoolController {

    private final CrmService crmService;
    private final CrmCurrentUserResolver currentUserResolver;

    @Operation(summary = "public pool list")
    @GetMapping("/list")
    public R<PageResult<CrmCustomerVO>> list(CrmPublicPoolQueryDTO query) {
        return R.ok(crmService.pagePublicPool(query, currentUserResolver.resolve()));
    }

    @Operation(summary = "claim customer")
    @PostMapping("/claim/{customerId}")
    public R<Void> claim(@PathVariable Long customerId) {
        crmService.claimCustomer(customerId, currentUserResolver.resolve());
        return R.ok();
    }

    @Operation(summary = "assign public pool customers")
    @PostMapping("/assign")
    public R<Void> assign(@Valid @RequestBody CrmCustomerAssignDTO request) {
        crmService.assignCustomers(request, currentUserResolver.resolve());
        return R.ok();
    }
}
