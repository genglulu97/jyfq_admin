package com.jyfq.loan.app.controller;

import com.jyfq.loan.common.result.R;
import com.jyfq.loan.app.util.ClientIpUtil;
import com.jyfq.loan.model.dto.CommonUpstreamEnvelopeDTO;
import com.jyfq.loan.model.dto.CommonUpstreamMobileEightEnvelopeDTO;
import com.jyfq.loan.model.dto.CommonUpstreamTestRequestDTO;
import com.jyfq.loan.service.upstream.CommonUpstreamGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "通用上游接入")
@RestController
@RequestMapping("/api/upstream/common")
@RequiredArgsConstructor
public class CommonUpstreamController {

    private final CommonUpstreamGatewayService gatewayService;

    @Operation(summary = "通用预检撞库")
    @PostMapping("/{scene}/preCheck")
    public R<Map<String, Object>> preCheck(@PathVariable String scene,
                                           @Valid @RequestBody CommonUpstreamEnvelopeDTO request,
                                           HttpServletRequest servletRequest) {
        return R.ok(gatewayService.preCheck(scene, request, ClientIpUtil.resolve(servletRequest)));
    }

    @Operation(summary = "8位掩码预检撞库")
    @PostMapping("/{scene}/mobileEight/preCheck")
    public R<Map<String, Object>> mobileEightPreCheck(@PathVariable String scene,
                                                      @Valid @RequestBody CommonUpstreamMobileEightEnvelopeDTO request,
                                                      HttpServletRequest servletRequest) {
        return R.ok(gatewayService.mobileEightPreCheck(scene, request, ClientIpUtil.resolve(servletRequest)));
    }

    @Operation(summary = "通用正式进件")
    @PostMapping("/{scene}/apply")
    public R<Map<String, Object>> apply(@PathVariable String scene,
                                        @Valid @RequestBody CommonUpstreamEnvelopeDTO request,
                                        HttpServletRequest servletRequest) {
        return R.ok(gatewayService.apply(scene, request, ClientIpUtil.resolve(servletRequest)));
    }

    @Operation(summary = "测试明文批量预检撞库")
    @PostMapping("/test/{scene}/preCheck")
    public R<Map<String, Object>> testPreCheck(@PathVariable String scene,
                                               @Valid @RequestBody CommonUpstreamTestRequestDTO request,
                                               HttpServletRequest servletRequest) {
        return R.ok(gatewayService.testPreCheck(scene, request, ClientIpUtil.resolve(servletRequest)));
    }

    @Operation(summary = "测试明文批量正式进件")
    @PostMapping("/test/{scene}/apply")
    public R<Map<String, Object>> testApply(@PathVariable String scene,
                                            @Valid @RequestBody CommonUpstreamTestRequestDTO request,
                                            HttpServletRequest servletRequest) {
        return R.ok(gatewayService.testApply(scene, request, ClientIpUtil.resolve(servletRequest)));
    }

    @Operation(summary = "测试明文批量预检撞库")
    @PostMapping("/test/{scene}/batch/preCheck")
    public R<List<Map<String, Object>>> testBatchPreCheck(@PathVariable String scene,
                                                          @RequestBody List<@Valid CommonUpstreamTestRequestDTO> request,
                                                          HttpServletRequest servletRequest) {
        return R.ok(gatewayService.testBatchPreCheck(scene, request, ClientIpUtil.resolve(servletRequest)));
    }

    @Operation(summary = "测试明文批量正式进件")
    @PostMapping("/test/{scene}/batch/apply")
    public R<List<Map<String, Object>>> testBatchApply(@PathVariable String scene,
                                                       @RequestBody List<@Valid CommonUpstreamTestRequestDTO> request,
                                                       HttpServletRequest servletRequest) {
        return R.ok(gatewayService.testBatchApply(scene, request, ClientIpUtil.resolve(servletRequest)));
    }
}
