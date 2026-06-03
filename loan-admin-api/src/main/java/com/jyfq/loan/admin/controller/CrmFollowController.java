package com.jyfq.loan.admin.controller;

import com.jyfq.loan.admin.support.CrmCurrentUserResolver;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.CrmFollowRecordQueryDTO;
import com.jyfq.loan.model.dto.CrmFollowRecordSaveDTO;
import com.jyfq.loan.model.vo.CrmFollowRecordVO;
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

@Tag(name = "CRM Follow")
@RestController
@RequestMapping("/admin/crm/follow")
@RequiredArgsConstructor
public class CrmFollowController {

    private final CrmService crmService;
    private final CrmCurrentUserResolver currentUserResolver;

    @Operation(summary = "follow record list")
    @GetMapping("/list")
    public R<PageResult<CrmFollowRecordVO>> list(CrmFollowRecordQueryDTO query) {
        return R.ok(crmService.pageFollowRecords(query, currentUserResolver.resolve()));
    }

    @Operation(summary = "add follow record")
    @PostMapping("/add")
    public R<Long> add(@Valid @RequestBody CrmFollowRecordSaveDTO request) {
        return R.ok(crmService.addFollowRecord(request, currentUserResolver.resolve()));
    }
}
