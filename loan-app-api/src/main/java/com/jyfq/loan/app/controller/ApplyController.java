package com.jyfq.loan.app.controller;

import cn.hutool.crypto.digest.DigestUtil;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.app.util.ClientIpUtil;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.common.result.ResultCode;
import com.jyfq.loan.model.dto.ApplyRequestDTO;
import com.jyfq.loan.model.dto.H5ApplyConfirmRequestDTO;
import com.jyfq.loan.model.dto.H5ApplyRequestDTO;
import com.jyfq.loan.model.dto.H5SmsCodeRequestDTO;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.model.entity.CollisionRecord;
import com.jyfq.loan.model.vo.H5UvProductVO;
import com.jyfq.loan.service.ApplyService;
import com.jyfq.loan.service.H5PromotionService;
import com.jyfq.loan.service.UvProductService;
import com.jyfq.loan.service.h5.H5ApplyMappingUtil;
import com.jyfq.loan.thirdparty.model.PreCheckResult;
import com.jyfq.loan.thirdparty.model.PushResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * H5 application APIs.
 */
@Tag(name = "Apply")
@RestController
@RequestMapping("/api/apply")
@RequiredArgsConstructor
public class ApplyController {

    private final ApplyService applyService;
    private final H5PromotionService h5PromotionService;
    private final UvProductService uvProductService;

    @Operation(summary = "Submit standard application")
    @PostMapping("/submit")
    public R<PreCheckResult> submit(@Valid @RequestBody ApplyRequestDTO dto, HttpServletRequest servletRequest) {
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
                .ip(dto.getDeviceIp())
                .build();

        PreCheckResult result = applyService.competitivePreCheck(data);
        h5PromotionService.trackComplete(dto.getChannelCode(), ClientIpUtil.resolve(servletRequest),
                servletRequest.getHeader("User-Agent"), servletRequest.getHeader("Referer"));
        return R.ok(result);
    }

    @Operation(summary = "Submit H5 form and run product pre-check")
    @PostMapping("/h5/submit")
    public R<PreCheckResult> h5Submit(@Valid @RequestBody H5ApplyRequestDTO dto, HttpServletRequest servletRequest) {
        String clientIp = ClientIpUtil.resolve(servletRequest);
        StandardApplyData data = H5ApplyMappingUtil.toStandardData(dto, clientIp);
        PreCheckResult result = applyService.competitivePreCheck(data);
        h5PromotionService.trackComplete(dto.getChannelCode(), clientIp,
                servletRequest.getHeader("User-Agent"), servletRequest.getHeader("Referer"));
        return R.ok(result);
    }

    @Operation(summary = "Confirm H5 authorization and submit to institution")
    @PostMapping("/h5/apply")
    public R<Map<String, Object>> h5Apply(@Valid @RequestBody H5ApplyConfirmRequestDTO dto,
                                          HttpServletRequest servletRequest) {
        String clientIp = ClientIpUtil.resolve(servletRequest);
        StandardApplyData data = H5ApplyMappingUtil.toStandardData(dto, clientIp);
        fillIdentity(data, dto);

        CollisionRecord matchedOrder = applyService.findMatchedCollisionRecord(data, dto.getLocalOrderNo());
        if (matchedOrder == null || matchedOrder.getProductId() == null) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "Please preCheck successfully before apply");
        }

        Long productId = dto.getProductId() == null ? matchedOrder.getProductId() : dto.getProductId();
        if (!Objects.equals(productId, matchedOrder.getProductId())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "Selected product is not the preCheck winner");
        }

        PushResult pushResult = applyService.pushToInstitution(data, productId, matchedOrder.getCollisionNo());
        return R.ok(buildH5ApplyResponse(dto.getChannelCode(), matchedOrder, pushResult));
    }

    @Operation(summary = "Send H5 SMS verification code")
    @PostMapping("/h5/sms-code")
    public R<?> sendH5SmsCode(@Valid @RequestBody H5SmsCodeRequestDTO dto) {
        return R.ok();
    }

    @Operation(summary = "List online UV products for H5")
    @GetMapping("/h5/uv-products")
    public R<List<H5UvProductVO>> h5UvProducts() {
        return R.ok(uvProductService.listH5Products());
    }

    @Operation(summary = "Query application status")
    @GetMapping("/status/{orderNo}")
    public R<?> status(@PathVariable String orderNo) {
        return R.ok();
    }

    private void fillIdentity(StandardApplyData data, H5ApplyConfirmRequestDTO dto) {
        String name = trimToNull(dto.getName());
        String idCard = trimToNull(dto.getIdCard());
        data.setName(name);
        data.setIdCard(idCard);
        data.setIdCardPrefixFour(idCard == null || idCard.length() < 4 ? null : idCard.substring(0, 4));
    }

    private Map<String, Object> buildH5ApplyResponse(String channelCode, CollisionRecord matchedOrder, PushResult pushResult) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("scene", "h5");
        response.put("channelCode", channelCode);
        response.put("localOrderNo", matchedOrder == null ? null : matchedOrder.getCollisionNo());
        response.put("productId", matchedOrder == null ? null : matchedOrder.getProductId());
        response.put("success", pushResult != null && pushResult.isSuccess());
        response.put("instCode", pushResult == null ? null : pushResult.getInstCode());
        response.put("thirdOrderNo", pushResult == null ? null : pushResult.getThirdOrderNo());
        response.put("message", pushResult == null ? "apply failed" : pushResult.getMsg());
        return response;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
