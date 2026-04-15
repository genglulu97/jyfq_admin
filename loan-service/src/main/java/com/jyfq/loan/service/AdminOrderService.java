package com.jyfq.loan.service;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.model.dto.OrderQueryDTO;
import com.jyfq.loan.model.vo.OrderDetailVO;
import com.jyfq.loan.model.vo.OrderListVO;
import com.jyfq.loan.model.vo.OrderPushRecordVO;

import java.util.List;

/**
 * Admin order query service.
 */
public interface AdminOrderService {

    PageResult<OrderListVO> pageOrders(OrderQueryDTO query);

    OrderDetailVO getOrderDetail(String orderNo);

    List<OrderPushRecordVO> listPushRecords(String orderNo);
}
