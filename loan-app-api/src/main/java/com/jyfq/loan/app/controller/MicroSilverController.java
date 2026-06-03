package com.jyfq.loan.app.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.jyfq.loan.app.util.ClientIpUtil;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.common.result.ResultCode;
import com.jyfq.loan.model.dto.StandardApplyData;
import com.jyfq.loan.service.ApplyService;
import com.jyfq.loan.service.strategy.UpstreamStrategy;
import com.jyfq.loan.service.strategy.UpstreamStrategyFactory;
import com.jyfq.loan.service.upstream.ChannelAccessGuard;
import com.jyfq.loan.thirdparty.model.PreCheckResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "微银信用适配接口")
@RestController
@RequestMapping("/api/upstream/microsilver")
@RequiredArgsConstructor
public class MicroSilverController {

    private final UpstreamStrategyFactory strategyFactory;
    private final ApplyService applyService;
    private final ChannelAccessGuard channelAccessGuard;

    @Operation(summary = "预申请撞库")
    @PostMapping("/preCheck")
    public R<?> preCheck(@RequestBody JSONObject request, HttpServletRequest servletRequest) {
        String orgCode = request.getString("orgCode");
        String data = request.getString("data");
        validateOuterRequest(orgCode, data);
        String clientIp = ClientIpUtil.resolve(servletRequest);
        channelAccessGuard.validate(orgCode, clientIp);

        log.info("[MICROSILVER] preCheck request orgCode={}, request={}", orgCode, JSON.toJSONString(request));

        UpstreamStrategy strategy = strategyFactory.getStrategy("microsilver");
        if (strategy == null) {
            throw new BizException(ResultCode.CHANNEL_NOT_FOUND, "microsilver");
        }

        StandardApplyData applyData = strategy.parseRequest(data);
        fillRequestIp(applyData, clientIp);
        PreCheckResult winner = applyService.competitivePreCheck(applyData);

        if (winner == null || !winner.isPass()) {
            return R.fail("未匹配到合适的产品");
        }

        JSONObject responseData = new JSONObject();
        responseData.put("platformName", "jyfq-platform");
        responseData.put("productName", winner.getInstCode() + "产品");
        responseData.put("companyName", winner.getInstCode());
        responseData.put("price", winner.getPrice().toPlainString());
        return R.ok(responseData);
    }

    @Operation(summary = "正式进件")
    @PostMapping("/apply")
    public R<?> apply(@RequestBody JSONObject request, HttpServletRequest servletRequest) {
        String orgCode = request.getString("orgCode");
        String data = request.getString("data");
        if (!StringUtils.hasText(data)) {
            throw new BizException(ResultCode.PARAM_MISSING, "data");
        }
        if (StringUtils.hasText(orgCode) && !"microsilver".equals(orgCode)) {
            throw new BizException(ResultCode.CHANNEL_NOT_FOUND, orgCode);
        }
        String clientIp = ClientIpUtil.resolve(servletRequest);
        channelAccessGuard.validate("microsilver", clientIp);

        UpstreamStrategy strategy = strategyFactory.getStrategy("microsilver");
        if (strategy == null) {
            throw new BizException(ResultCode.CHANNEL_NOT_FOUND, "microsilver");
        }

        StandardApplyData applyData = strategy.parseRequest(data);
        fillRequestIp(applyData, clientIp);
        Long productId = 1L;
        com.jyfq.loan.thirdparty.model.PushResult pushResult = applyService.pushToInstitution(applyData, productId);

        if (pushResult.isSuccess()) {
            return R.ok("进件成功", pushResult.getMsg());
        }
        return R.fail("进件失败: " + pushResult.getMsg());
    }

    private void fillRequestIp(StandardApplyData applyData, String clientIp) {
        if (applyData != null && !StringUtils.hasText(applyData.getIp())) {
            applyData.setIp(clientIp);
        }
    }

    private void validateOuterRequest(String orgCode, String data) {
        if (!StringUtils.hasText(orgCode)) {
            throw new BizException(ResultCode.PARAM_MISSING, "orgCode");
        }
        if (!"microsilver".equals(orgCode)) {
            throw new BizException(ResultCode.CHANNEL_NOT_FOUND, orgCode);
        }
        if (!StringUtils.hasText(data)) {
            throw new BizException(ResultCode.PARAM_MISSING, "data");
        }
    }
}
