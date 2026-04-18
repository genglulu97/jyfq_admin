package com.jyfq.loan.admin.controller;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.InstitutionApiConfigUpdateDTO;
import com.jyfq.loan.model.dto.InstitutionQueryDTO;
import com.jyfq.loan.model.dto.InstitutionRechargeDTO;
import com.jyfq.loan.model.dto.InstitutionSaveDTO;
import com.jyfq.loan.model.dto.InstitutionStatusUpdateDTO;
import com.jyfq.loan.model.vo.InstitutionApiConfigDetailVO;
import com.jyfq.loan.model.vo.InstitutionApiConfigListVO;
import com.jyfq.loan.model.vo.InstitutionApiConfigOptionsVO;
import com.jyfq.loan.model.vo.InstitutionDetailVO;
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

    @Operation(summary = "机构API配置列表")
    @GetMapping("/api-config/list")
    public R<PageResult<InstitutionApiConfigListVO>> apiConfigList(InstitutionQueryDTO query) {
        return R.ok(adminInstitutionService.pageInstitutionApiConfigs(query));
    }

    @Operation(summary = "新增机构")
    @PostMapping("/add")
    public R<Long> add(@Valid @RequestBody InstitutionSaveDTO request) {
        return R.ok(adminInstitutionService.createInstitution(request));
    }

    @Operation(summary = "机构详情")
    @GetMapping("/detail/{id}")
    public R<InstitutionDetailVO> detail(@PathVariable Long id) {
        return R.ok(adminInstitutionService.getInstitutionDetail(id));
    }

    @Operation(summary = "机构API配置详情")
    @GetMapping("/api-config/detail/{id}")
    public R<InstitutionApiConfigDetailVO> apiConfigDetail(@PathVariable Long id) {
        return R.ok(adminInstitutionService.getInstitutionApiConfigDetail(id));
    }

    @Operation(summary = "鏈烘瀯API閰嶇疆閫夐」")
    @GetMapping("/api-config/options")
    public R<InstitutionApiConfigOptionsVO> apiConfigOptions() {
        return R.ok(adminInstitutionService.getInstitutionApiConfigOptions());
    }

    @Operation(summary = "修改机构")
    @PutMapping("/update/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody InstitutionSaveDTO request) {
        adminInstitutionService.updateInstitution(id, request);
        return R.ok();
    }

    @Operation(summary = "修改机构API配置")
    @PutMapping("/api-config/update/{id}")
    public R<?> updateApiConfig(@PathVariable Long id, @Valid @RequestBody InstitutionApiConfigUpdateDTO request) {
        adminInstitutionService.updateInstitutionApiConfig(id, request);
        return R.ok();
    }

    @Operation(summary = "修改机构状态")
    @PutMapping("/api-config/status/{id}")
    public R<?> updateStatus(@PathVariable Long id, @Valid @RequestBody InstitutionStatusUpdateDTO request) {
        adminInstitutionService.updateInstitutionStatus(id, request);
        return R.ok();
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
