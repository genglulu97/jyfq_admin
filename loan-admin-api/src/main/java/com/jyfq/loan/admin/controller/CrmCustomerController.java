package com.jyfq.loan.admin.controller;

import com.jyfq.loan.admin.support.CrmCurrentUserResolver;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.CrmCustomerAssignDTO;
import com.jyfq.loan.model.dto.CrmCustomerBatchImportDTO;
import com.jyfq.loan.model.dto.CrmCustomerQueryDTO;
import com.jyfq.loan.model.dto.CrmCustomerSaveDTO;
import com.jyfq.loan.model.vo.CrmCustomerDetailVO;
import com.jyfq.loan.model.vo.CrmCustomerVO;
import com.jyfq.loan.model.vo.CrmImportResultVO;
import com.jyfq.loan.service.CrmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "CRM Customer")
@RestController
@RequestMapping("/admin/crm/customer")
@RequiredArgsConstructor
public class CrmCustomerController {

    private final CrmService crmService;
    private final CrmCurrentUserResolver currentUserResolver;

    @Operation(summary = "customer list")
    @GetMapping("/list")
    public R<PageResult<CrmCustomerVO>> list(CrmCustomerQueryDTO query) {
        return R.ok(crmService.pageCustomers(query, currentUserResolver.resolve()));
    }

    @Operation(summary = "my customer list")
    @GetMapping("/my")
    public R<PageResult<CrmCustomerVO>> my(CrmCustomerQueryDTO query) {
        return R.ok(crmService.pageMyCustomers(query, currentUserResolver.resolve()));
    }

    @Operation(summary = "customer detail")
    @GetMapping("/detail/{id}")
    public R<CrmCustomerDetailVO> detail(@PathVariable Long id) {
        return R.ok(crmService.customerDetail(id, currentUserResolver.resolve()));
    }

    @Operation(summary = "add customer")
    @PostMapping("/add")
    public R<Long> add(@Valid @RequestBody CrmCustomerSaveDTO request) {
        return R.ok(crmService.createCustomer(request, currentUserResolver.resolve()));
    }

    @Operation(summary = "update customer")
    @PutMapping("/update/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CrmCustomerSaveDTO request) {
        crmService.updateCustomer(id, request, currentUserResolver.resolve());
        return R.ok();
    }

    @Operation(summary = "delete customer")
    @DeleteMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Long id) {
        crmService.deleteCustomer(id, currentUserResolver.resolve());
        return R.ok();
    }

    @Operation(summary = "assign customers")
    @PostMapping("/assign")
    public R<Void> assign(@Valid @RequestBody CrmCustomerAssignDTO request) {
        crmService.assignCustomers(request, currentUserResolver.resolve());
        return R.ok();
    }

    @Operation(summary = "reclaim customers to public pool")
    @PostMapping("/reclaim")
    public R<Void> reclaim(@RequestParam(required = false) String reason, @RequestBody List<Long> customerIds) {
        crmService.reclaimCustomers(customerIds, reason, currentUserResolver.resolve());
        return R.ok();
    }

    @Operation(summary = "batch import customers")
    @PostMapping("/batch-import")
    public R<CrmImportResultVO> batchImport(@RequestBody CrmCustomerBatchImportDTO request) {
        return R.ok(crmService.batchImportCustomers(request, currentUserResolver.resolve()));
    }
}
