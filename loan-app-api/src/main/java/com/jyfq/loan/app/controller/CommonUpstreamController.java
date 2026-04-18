package com.jyfq.loan.app.controller;

import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.CommonUpstreamEnvelopeDTO;
import com.jyfq.loan.service.upstream.CommonUpstreamGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                                           @Valid @RequestBody CommonUpstreamEnvelopeDTO request) {
        return R.ok(gatewayService.preCheck(scene, request));
    }

    @Operation(summary = "通用正式进件")
    @PostMapping("/{scene}/apply")
    public R<Map<String, Object>> apply(@PathVariable String scene,
                                        @Valid @RequestBody CommonUpstreamEnvelopeDTO request) {
        return R.ok(gatewayService.apply(scene, request));
    }
}
