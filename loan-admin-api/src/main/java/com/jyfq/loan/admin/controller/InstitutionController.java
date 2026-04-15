package com.jyfq.loan.admin.controller;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.InstitutionQueryDTO;
import com.jyfq.loan.model.dto.InstitutionRechargeDTO;
import com.jyfq.loan.model.dto.InstitutionSaveDTO;
import com.jyfq.loan.model.vo.InstitutionListVO;
import com.jyfq.loan.model.vo.InstitutionProductVO;
import com.jyfq.loan.model.vo.InstitutionRechargeRecordVO;
import com.jyfq.loan.model.vo.OptionVO;
import com.jyfq.loan.service.AdminInstitutionService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin institution management APIs.
 */
@Tag(name = "机构管理")
@RestController
@RequestMapping("/admin/institution")
@RequiredArgsConstructor
public class InstitutionController {

    private final AdminInstitutionService adminInstitutionService;

    @Operation(summary = "机构列表")
    @GetMapping("/list")
    public R<PageResult<InstitutionListVO>> list(InstitutionQueryDTO query) {
        return R.ok(adminInstitutionService.pageInstitutions(query));
    }

    @Operation(summary = "新增机构")
    @PostMapping("/add")
    public R<Long> add(@Valid @RequestBody InstitutionSaveDTO request) {
        return R.ok(adminInstitutionService.createInstitution(request));
    }

    @Operation(summary = "删除机构")
    @DeleteMapping("/delete/{id}")
    public R<?> delete(@PathVariable Long id) {
        adminInstitutionService.deleteInstitution(id);
        return R.ok();
    }

    @Operation(summary = "开放城市下拉")
    @GetMapping("/city-options")
    public R<List<OptionVO>> cityOptions() {
        return R.ok(adminInstitutionService.listCityOptions());
    }

    @Operation(summary = "机构产品配置")
    @GetMapping("/products/{instId}")
    public R<List<InstitutionProductVO>> products(@PathVariable Long instId) {
        return R.ok(adminInstitutionService.listProducts(instId));
    }

    @Operation(summary = "充值记录")
    @GetMapping("/recharge-records/{instId}")
    public R<List<InstitutionRechargeRecordVO>> rechargeRecords(@PathVariable Long instId) {
        return R.ok(adminInstitutionService.listRechargeRecords(instId));
    }

    @Operation(summary = "机构启停切换")
    @PutMapping("/toggle/{instId}")
    public R<?> toggle(@PathVariable Long instId) {
        adminInstitutionService.toggleInstitution(instId);
        return R.ok();
    }

    @Operation(summary = "商户充值")
    @PostMapping("/recharge/{instId}")
    public R<?> recharge(@PathVariable Long instId,
                         @Valid @RequestBody InstitutionRechargeDTO request) {
        adminInstitutionService.recharge(instId, request);
        return R.ok();
    }
}
