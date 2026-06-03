package com.jyfq.loan.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jyfq.loan.common.result.R;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.mapper.InstitutionProductMapper;
import com.jyfq.loan.model.entity.ApplyOrder;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.entity.InstitutionProduct;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin dashboard APIs.
 */
@Tag(name = "Admin Dashboard")
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ApplyOrderMapper applyOrderMapper;
    private final ChannelMapper channelMapper;
    private final InstitutionMapper institutionMapper;
    private final InstitutionProductMapper institutionProductMapper;

    @Operation(summary = "Dashboard KPI")
    @GetMapping("/kpi")
    public R<Map<String, Object>> kpi() {
        long totalOrders = applyOrderMapper.selectCount(new LambdaQueryWrapper<ApplyOrder>());
        long pendingOrders = countOrdersByStatus(0);
        long successOrders = countOrdersByStatus(1);
        long failedOrders = countOrdersByStatus(9);
        long activeChannels = channelMapper.selectCount(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getStatus, 1));
        long activeInstitutions = institutionMapper.selectCount(new LambdaQueryWrapper<Institution>()
                .eq(Institution::getStatus, 1));
        long activeProducts = institutionProductMapper.selectCount(new LambdaQueryWrapper<InstitutionProduct>()
                .eq(InstitutionProduct::getStatus, 1));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalOrders", totalOrders);
        data.put("orderCount", totalOrders);
        data.put("pendingOrders", pendingOrders);
        data.put("successOrders", successOrders);
        data.put("failedOrders", failedOrders);
        data.put("successRate", percentage(successOrders, totalOrders));
        data.put("activeChannels", activeChannels);
        data.put("activeInstitutions", activeInstitutions);
        data.put("activeProducts", activeProducts);
        data.put("settlementAmount", sumSettlementAmount());
        return R.ok(data);
    }

    @Operation(summary = "Dashboard funnel")
    @GetMapping("/funnel")
    public R<List<Map<String, Object>>> funnel() {
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(funnelItem("Pending", "pending", countOrdersByStatus(0)));
        data.add(funnelItem("Pushing", "pushing", countOrdersByStatus(1)));
        data.add(funnelItem("Crediting", "crediting", countOrdersByStatus(2)));
        data.add(funnelItem("Loaned", "loaned", countOrdersByStatus(3)));
        data.add(funnelItem("Failed", "failed", countOrdersByStatus(9)));
        return R.ok(data);
    }

    @Operation(summary = "Dashboard alerts")
    @GetMapping("/alerts")
    public R<List<Map<String, Object>>> alerts() {
        List<ApplyOrder> orders = applyOrderMapper.selectList(new LambdaQueryWrapper<ApplyOrder>()
                .eq(ApplyOrder::getOrderStatus, 9)
                .isNotNull(ApplyOrder::getRejectReason)
                .orderByDesc(ApplyOrder::getCreatedAt)
                .last("LIMIT 10"));

        List<Map<String, Object>> data = new ArrayList<>();
        for (ApplyOrder order : orders) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", order.getId());
            item.put("orderNo", order.getOrderNo());
            item.put("title", "Order failed");
            item.put("content", order.getRejectReason());
            item.put("reason", order.getRejectReason());
            item.put("level", "warning");
            item.put("createdAt", order.getCreatedAt());
            data.add(item);
        }
        return R.ok(data);
    }

    @Operation(summary = "Dashboard channel ranks")
    @GetMapping("/channel-ranks")
    public R<List<Map<String, Object>>> channelRanks() {
        List<Map<String, Object>> rows = applyOrderMapper.selectMaps(new QueryWrapper<ApplyOrder>()
                .select("channel_id AS channelId",
                        "channel_code AS channelCode",
                        "COUNT(*) AS orderCount",
                        "SUM(CASE WHEN order_status = 1 THEN 1 ELSE 0 END) AS successCount",
                        "SUM(CASE WHEN order_status = 9 THEN 1 ELSE 0 END) AS failedCount",
                        "COALESCE(SUM(settlement_price), 0) AS settlementAmount")
                .isNotNull("channel_id")
                .groupBy("channel_id", "channel_code")
                .orderByDesc("orderCount")
                .last("LIMIT 10"));

        for (Map<String, Object> row : rows) {
            long orderCount = toLong(row.get("orderCount"));
            long successCount = toLong(row.get("successCount"));
            row.put("name", row.get("channelCode"));
            row.put("successRate", percentage(successCount, orderCount));
        }
        return R.ok(rows);
    }

    private long countOrdersByStatus(int status) {
        return applyOrderMapper.selectCount(new LambdaQueryWrapper<ApplyOrder>()
                .eq(ApplyOrder::getOrderStatus, status));
    }

    private BigDecimal sumSettlementAmount() {
        List<Map<String, Object>> rows = applyOrderMapper.selectMaps(new QueryWrapper<ApplyOrder>()
                .select("COALESCE(SUM(settlement_price), 0) AS settlementAmount"));
        if (rows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Object value = rows.get(0).get("settlementAmount");
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal amount ? amount : new BigDecimal(String.valueOf(value));
    }

    private Map<String, Object> funnelItem(String name, String key, long value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("key", key);
        item.put("value", value);
        item.put("count", value);
        return item;
    }

    private BigDecimal percentage(long part, long total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP);
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
