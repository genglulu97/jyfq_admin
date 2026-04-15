package com.jyfq.loan.admin.controller;

import com.jyfq.loan.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 报表统计接口
 */
@Tag(name = "报表统计")
@RestController
@RequestMapping("/admin/report")
@RequiredArgsConstructor
public class ReportController {

    @Operation(summary = "小时统计数据")
    @GetMapping("/hourly")
    public R<?> hourly(@RequestParam String date,
                       @RequestParam(required = false) Long channelId) {
        return R.ok();
    }

    @Operation(summary = "渠道汇总统计")
    @GetMapping("/channel-summary")
    public R<?> channelSummary(@RequestParam String startDate,
                               @RequestParam String endDate) {
        return R.ok();
    }
}
