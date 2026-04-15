package com.jyfq.loan.admin.controller;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.ChannelQueryDTO;
import com.jyfq.loan.model.dto.ChannelSaveDTO;
import com.jyfq.loan.model.vo.ChannelListVO;
import com.jyfq.loan.service.AdminChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin channel management APIs.
 */
@Tag(name = "渠道管理")
@RestController
@RequestMapping("/admin/channel")
@RequiredArgsConstructor
public class ChannelController {

    private final AdminChannelService adminChannelService;

    @Operation(summary = "渠道列表")
    @GetMapping("/list")
    public R<PageResult<ChannelListVO>> list(ChannelQueryDTO query) {
        return R.ok(adminChannelService.pageChannels(query));
    }

    @Operation(summary = "新增渠道")
    @PostMapping("/add")
    public R<Long> add(@Valid @RequestBody ChannelSaveDTO request) {
        return R.ok(adminChannelService.createChannel(request));
    }

    @Operation(summary = "更新渠道")
    @PutMapping("/update")
    public R<?> update(@RequestParam Long id, @Valid @RequestBody ChannelSaveDTO request) {
        adminChannelService.updateChannel(id, request);
        return R.ok();
    }

    @Operation(summary = "启停渠道")
    @PutMapping("/toggle/{id}")
    public R<?> toggle(@PathVariable Long id) {
        adminChannelService.toggleChannel(id);
        return R.ok();
    }
}
