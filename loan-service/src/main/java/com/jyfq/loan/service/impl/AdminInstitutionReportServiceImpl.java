package com.jyfq.loan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.mapper.DeductionRecordMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.model.dto.InstitutionReportQueryDTO;
import com.jyfq.loan.model.entity.ApplyOrder;
import com.jyfq.loan.model.entity.DeductionRecord;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.vo.InstitutionReportRowVO;
import com.jyfq.loan.model.vo.InstitutionReportSummaryVO;
import com.jyfq.loan.service.AdminInstitutionReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin institution report service implementation.
 */
@Service
@RequiredArgsConstructor
public class AdminInstitutionReportServiceImpl implements AdminInstitutionReportService {

    private static final Set<Integer> SUCCESS_ORDER_STATUSES = Set.of(1, 2, 3);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ApplyOrderMapper applyOrderMapper;
    private final InstitutionMapper institutionMapper;
    private final DeductionRecordMapper deductionRecordMapper;

    @Override
    public InstitutionReportSummaryVO summary(InstitutionReportQueryDTO query) {
        List<InstitutionReportRowVO> rows = buildRows(query);
        InstitutionReportSummaryVO summary = new InstitutionReportSummaryVO();
        summary.setTotalOrders(rows.stream().mapToLong(InstitutionReportRowVO::getTotalOrders).sum());
        summary.setSuccessOrders(rows.stream().mapToLong(InstitutionReportRowVO::getSuccessOrders).sum());
        summary.setSuccessRate(calculateRate(summary.getSuccessOrders(), summary.getTotalOrders()));
        summary.setTotalAmount(rows.stream()
                .map(InstitutionReportRowVO::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setDeductAmount(rows.stream()
                .map(InstitutionReportRowVO::getDeductAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return summary;
    }

    @Override
    public PageResult<InstitutionReportRowVO> page(InstitutionReportQueryDTO query) {
        long current = normalizePageNo(query == null ? null : query.getCurrent());
        long size = normalizePageSize(query == null ? null : query.getSize());
        List<InstitutionReportRowVO> rows = buildRows(query);
        if (rows.isEmpty()) {
            return PageResult.empty(current, size);
        }

        int fromIndex = (int) Math.min((current - 1) * size, rows.size());
        int toIndex = (int) Math.min(fromIndex + size, rows.size());
        return PageResult.of(current, size, rows.size(), rows.subList(fromIndex, toIndex));
    }

    @Override
    public List<InstitutionReportRowVO> exportRows(InstitutionReportQueryDTO query) {
        return buildRows(query);
    }

    private List<InstitutionReportRowVO> buildRows(InstitutionReportQueryDTO query) {
        InstitutionReportQueryDTO safeQuery = query == null ? new InstitutionReportQueryDTO() : query;
        List<ApplyOrder> orders = applyOrderMapper.selectList(buildOrderQueryWrapper(safeQuery));
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Institution> institutionMap = buildInstitutionMap(orders.stream()
                .map(ApplyOrder::getInstId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        Map<String, DeductionRecord> deductionRecordMap = buildLatestDeductionRecordMap(orders.stream()
                .map(ApplyOrder::getOrderNo)
                .collect(Collectors.toList()));

        Map<Long, InstitutionReportRowVO> rowMap = new LinkedHashMap<>();
        for (ApplyOrder order : orders) {
            if (order.getInstId() == null) {
                continue;
            }
            Institution institution = institutionMap.get(order.getInstId());
            InstitutionReportRowVO row = rowMap.computeIfAbsent(order.getInstId(),
                    instId -> createRow(instId, institution));
            row.setTotalOrders(row.getTotalOrders() + 1);
            if (isSuccess(order.getOrderStatus())) {
                row.setSuccessOrders(row.getSuccessOrders() + 1);
                row.setTotalAmount(row.getTotalAmount().add(nullToZero(order.getSettlementPrice())));
                row.setDeductAmount(row.getDeductAmount().add(resolveDeductAmount(order,
                        deductionRecordMap.get(order.getOrderNo()))));
            }
        }

        return rowMap.values().stream()
                .peek(row -> row.setSuccessRate(calculateRate(row.getSuccessOrders(), row.getTotalOrders())))
                .sorted(Comparator.comparing(InstitutionReportRowVO::getTotalOrders).reversed()
                        .thenComparing(InstitutionReportRowVO::getInstId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private LambdaQueryWrapper<ApplyOrder> buildOrderQueryWrapper(InstitutionReportQueryDTO query) {
        LambdaQueryWrapper<ApplyOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(ApplyOrder::getOrderNo,
                        ApplyOrder::getInstId,
                        ApplyOrder::getOrderStatus,
                        ApplyOrder::getSettlementPrice,
                        ApplyOrder::getCreatedAt)
                .isNotNull(ApplyOrder::getInstId);
        if (query.getInstId() != null) {
            wrapper.eq(ApplyOrder::getInstId, query.getInstId());
        }
        LocalDateTime start = parseStart(query.getStartDate());
        LocalDateTime end = parseEnd(query.getEndDate());
        if (start != null) {
            wrapper.ge(ApplyOrder::getCreatedAt, start);
        }
        if (end != null) {
            wrapper.le(ApplyOrder::getCreatedAt, end);
        }
        return wrapper.orderByDesc(ApplyOrder::getCreatedAt);
    }

    private InstitutionReportRowVO createRow(Long instId, Institution institution) {
        InstitutionReportRowVO row = new InstitutionReportRowVO();
        row.setInstId(instId);
        if (institution != null) {
            row.setInstCode(institution.getInstCode());
            row.setInstName(institution.getInstName());
            row.setMerchantAlias(institution.getMerchantAlias());
        }
        return row;
    }

    private Map<Long, Institution> buildInstitutionMap(List<Long> instIds) {
        return buildMap(instIds, institutionMapper::selectBatchIds, Institution::getId);
    }

    private <K, T> Map<K, T> buildMap(List<K> ids, Function<Collection<K>, List<T>> query,
                                      Function<T, K> keyMapper) {
        List<K> distinctIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return query.apply(distinctIds).stream()
                .collect(Collectors.toMap(keyMapper, Function.identity(), (left, right) -> left));
    }

    private Map<String, DeductionRecord> buildLatestDeductionRecordMap(List<String> orderNos) {
        List<String> distinctOrderNos = orderNos.stream()
                .filter(orderNo -> orderNo != null && !orderNo.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (distinctOrderNos.isEmpty()) {
            return Collections.emptyMap();
        }

        List<DeductionRecord> records = deductionRecordMapper.selectList(new LambdaQueryWrapper<DeductionRecord>()
                .select(DeductionRecord::getOrderNo,
                        DeductionRecord::getDeductType,
                        DeductionRecord::getAmount,
                        DeductionRecord::getStatus,
                        DeductionRecord::getCreatedAt)
                .in(DeductionRecord::getOrderNo, distinctOrderNos)
                .eq(DeductionRecord::getStatus, 1)
                .orderByDesc(DeductionRecord::getCreatedAt));

        Map<String, DeductionRecord> latestMap = new LinkedHashMap<>();
        for (DeductionRecord record : records) {
            DeductionRecord current = latestMap.get(record.getOrderNo());
            if (current == null || shouldReplace(record, current)) {
                latestMap.put(record.getOrderNo(), record);
            }
        }
        return latestMap;
    }

    private boolean shouldReplace(DeductionRecord candidate, DeductionRecord current) {
        boolean candidatePushSuccess = Integer.valueOf(3).equals(candidate.getDeductType());
        boolean currentPushSuccess = Integer.valueOf(3).equals(current.getDeductType());
        if (candidatePushSuccess != currentPushSuccess) {
            return candidatePushSuccess;
        }
        LocalDateTime candidateTime = candidate.getCreatedAt();
        return candidateTime != null && (current.getCreatedAt() == null || candidateTime.isAfter(current.getCreatedAt()));
    }

    private BigDecimal resolveDeductAmount(ApplyOrder order, DeductionRecord deductionRecord) {
        if (deductionRecord != null && deductionRecord.getAmount() != null) {
            return deductionRecord.getAmount();
        }
        return nullToZero(order.getSettlementPrice());
    }

    private boolean isSuccess(Integer orderStatus) {
        return orderStatus != null && SUCCESS_ORDER_STATUSES.contains(orderStatus);
    }

    private String calculateRate(long successCount, long totalCount) {
        if (totalCount <= 0) {
            return "0.00%";
        }
        return BigDecimal.valueOf(successCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private LocalDateTime parseStart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 10) {
            return LocalDate.parse(trimmed).atStartOfDay();
        }
        return LocalDateTime.parse(normalizeDateTime(trimmed), DATE_TIME_FORMATTER);
    }

    private LocalDateTime parseEnd(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 10) {
            return LocalDate.parse(trimmed).atTime(LocalTime.MAX.withNano(0));
        }
        return LocalDateTime.parse(normalizeDateTime(trimmed), DATE_TIME_FORMATTER);
    }

    private String normalizeDateTime(String value) {
        return value.length() == 16 ? value + ":00" : value;
    }

    private long normalizePageNo(Long current) {
        return current == null || current < 1 ? 1L : current;
    }

    private long normalizePageSize(Long size) {
        if (size == null || size < 1) {
            return 20L;
        }
        return Math.min(size, 100L);
    }
}
