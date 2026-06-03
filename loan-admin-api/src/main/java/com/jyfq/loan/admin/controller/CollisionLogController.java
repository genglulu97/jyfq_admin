package com.jyfq.loan.admin.controller;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.CollisionLogQueryDTO;
import com.jyfq.loan.model.vo.CollisionLogDetailVO;
import com.jyfq.loan.model.vo.CollisionLogListVO;
import com.jyfq.loan.service.AdminCollisionLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin collision log APIs.
 */
@Tag(name = "撞库日志")
@RestController
@RequestMapping({"/admin/collision-log", "/admin/collision"})
@RequiredArgsConstructor
public class CollisionLogController {

    private final AdminCollisionLogService adminCollisionLogService;

    @Operation(summary = "撞库日志列表")
    @GetMapping("/list")
    public R<PageResult<CollisionLogListVO>> list(CollisionLogQueryDTO query) {
        return R.ok(adminCollisionLogService.pageCollisionLogs(query));
    }

    @Operation(summary = "Collision log details")
    @GetMapping("/detail/{collisionNo}")
    public R<List<CollisionLogDetailVO>> detail(@PathVariable String collisionNo) {
        return R.ok(adminCollisionLogService.listCollisionLogDetails(collisionNo));
    }
}
