package com.jyfq.loan.app.controller;

import com.jyfq.loan.app.util.ClientIpUtil;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.H5TrackDTO;
import com.jyfq.loan.model.vo.H5IpCityVO;
import com.jyfq.loan.service.H5LocationService;
import com.jyfq.loan.service.H5PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * H5 promotion tracking APIs.
 */
@Tag(name = "H5 tracking")
@RestController
@RequestMapping("/api/h5")
@RequiredArgsConstructor
public class H5TrackController {

    private final H5PromotionService h5PromotionService;
    private final H5LocationService h5LocationService;

    @Operation(summary = "Track H5 event")
    @PostMapping("/track")
    public R<?> track(@Valid @RequestBody H5TrackDTO request, HttpServletRequest servletRequest) {
        h5PromotionService.track(request, ClientIpUtil.resolve(servletRequest),
                servletRequest.getHeader("User-Agent"), servletRequest.getHeader("Referer"));
        return R.ok();
    }

    @Operation(summary = "Track H5 event by query")
    @GetMapping("/track")
    public R<?> trackByQuery(@RequestParam String channelCode,
                             @RequestParam(defaultValue = "PV") String eventType,
                             @RequestParam(required = false) String visitorId,
                             @RequestParam(required = false) String sessionId,
                             @RequestParam(required = false) String pageUrl,
                             HttpServletRequest servletRequest) {
        H5TrackDTO request = new H5TrackDTO();
        request.setChannelCode(channelCode);
        request.setEventType(eventType);
        request.setVisitorId(visitorId);
        request.setSessionId(sessionId);
        request.setPageUrl(pageUrl);
        h5PromotionService.track(request, ClientIpUtil.resolve(servletRequest),
                servletRequest.getHeader("User-Agent"), servletRequest.getHeader("Referer"));
        return R.ok();
    }

    @Operation(summary = "Resolve H5 city by client IP")
    @GetMapping("/ip-city")
    public R<H5IpCityVO> ipCity(@RequestParam(required = false) String ip,
                                HttpServletRequest servletRequest) {
        String resolvedIp = StringUtils.hasText(ip) ? ip.trim() : ClientIpUtil.resolve(servletRequest);
        return R.ok(h5LocationService.resolveIpCity(resolvedIp));
    }
}
