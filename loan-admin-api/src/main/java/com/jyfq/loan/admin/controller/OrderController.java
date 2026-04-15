package com.jyfq.loan.admin.controller;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.model.dto.OrderQueryDTO;
import com.jyfq.loan.model.vo.OrderDetailVO;
import com.jyfq.loan.model.vo.OrderListVO;
import com.jyfq.loan.model.vo.OrderPushRecordVO;
import com.jyfq.loan.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin order management APIs.
 */
@Tag(name = "订单管理")
@RestController
@RequestMapping("/admin/order")
@RequiredArgsConstructor
public class OrderController {

    private final AdminOrderService adminOrderService;

    @Operation(summary = "订单列表")
    @GetMapping("/list")
    public R<PageResult<OrderListVO>> list(OrderQueryDTO query) {
        return R.ok(adminOrderService.pageOrders(query));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/detail/{orderNo}")
    public R<OrderDetailVO> detail(@PathVariable String orderNo) {
        return R.ok(adminOrderService.getOrderDetail(orderNo));
    }

    @Operation(summary = "推单记录")
    @GetMapping("/push-records/{orderNo}")
    public R<List<OrderPushRecordVO>> pushRecords(@PathVariable String orderNo) {
        return R.ok(adminOrderService.listPushRecords(orderNo));
    }
}
