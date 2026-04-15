package com.jyfq.loan.app.controller;

import cn.hutool.crypto.digest.DigestUtil;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.ApplyRequestDTO;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.service.ApplyService;
import com.jyfq.loan.thirdparty.model.PreCheckResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * H5 application APIs.
 */
@Tag(name = "进件接口")
@RestController
@RequestMapping("/api/apply")
@RequiredArgsConstructor
public class ApplyController {

    private final ApplyService applyService;

    @Operation(summary = "提交进件申请")
    @PostMapping("/submit")
    public R<PreCheckResult> submit(@Valid @RequestBody ApplyRequestDTO dto) {
        StandardApplyData data = StandardApplyData.builder()
                .channelCode(dto.getChannelCode())
                .name(dto.getUserName())
                .phone(dto.getPhone())
                .phoneMd5(DigestUtil.md5Hex(dto.getPhone()))
                .idCard(dto.getIdCard())
                .age(dto.getAge())
                .cityCode(dto.getCityCode())
                .workCity(dto.getWorkCity())
                .gender(dto.getGender())
                .profession(dto.getProfession())
                .zhima(dto.getZhima())
                .house(dto.getHouse())
                .vehicle(dto.getVehicle())
                .vehicleStatus(dto.getVehicleStatus())
                .vehicleValue(dto.getVehicleValue())
                .providentFund(dto.getProvidentFund())
                .socialSecurity(dto.getSocialSecurity())
                .commercialInsurance(dto.getCommercialInsurance())
                .overdue(dto.getOverdue())
                .loanAmount(dto.getAmount())
                .loanTime(dto.getLoanTime())
                .customerLevel(dto.getCustomerLevel())
                .ip(dto.getDeviceIp())
                .build();

        return R.ok(applyService.competitivePreCheck(data));
    }

    @Operation(summary = "查询申请状态")
    @GetMapping("/status/{orderNo}")
    public R<?> status(@PathVariable String orderNo) {
        return R.ok();
    }
}
