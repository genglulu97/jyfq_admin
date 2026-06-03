package com.jyfq.loan.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.util.AesUtil;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.mapper.DeductionRecordMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.mapper.InstitutionProductMapper;
import com.jyfq.loan.model.dto.DeductionRecordQueryDTO;
import com.jyfq.loan.model.entity.ApplyOrder;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.entity.DeductionRecord;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.entity.InstitutionProduct;
import com.jyfq.loan.model.vo.DeductionRecordListVO;
import com.jyfq.loan.model.vo.DeductionRecordSummaryVO;
import com.jyfq.loan.service.AdminDeductionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin deduction record query service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDeductionRecordServiceImpl implements AdminDeductionRecordService {

    private static final int FINANCE_STATUS_UNDEDUCTED = 0;
    private static final int FINANCE_STATUS_DEDUCTED = 1;
    private static final Set<Integer> DEDUCTED_ORDER_STATUSES = Set.of(1, 2, 3);
    private static final int FAILED_ORDER_STATUS = 9;

    private final ApplyOrderMapper applyOrderMapper;
    private final ChannelMapper channelMapper;
    private final InstitutionMapper institutionMapper;
    private final InstitutionProductMapper institutionProductMapper;
    private final DeductionRecordMapper deductionRecordMapper;

    @Override
    public PageResult<DeductionRecordListVO> pageDeductionRecords(DeductionRecordQueryDTO query) {
        DeductionRecordQueryDTO safeQuery = query == null ? new DeductionRecordQueryDTO() : query;
        long current = normalizePageNo(safeQuery.getCurrent());
        long size = normalizePageSize(safeQuery.getSize());

        Page<ApplyOrder> page = applyOrderMapper.selectPage(new Page<>(current, size),
                buildOrderQueryWrapper(safeQuery, true));
        if (page.getRecords().isEmpty()) {
            return PageResult.empty(current, size);
        }

        Map<Long, Channel> channelMap = buildChannelMap(page.getRecords());
        Map<Long, Institution> institutionMap = buildInstitutionMap(page.getRecords().stream()
                .map(ApplyOrder::getInstId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        Map<Long, InstitutionProduct> productMap = buildProductMap(page.getRecords());
        Map<String, DeductionRecord> deductionRecordMap = buildLatestDeductionRecordMap(page.getRecords().stream()
                .map(ApplyOrder::getOrderNo)
                .collect(Collectors.toList()));

        List<DeductionRecordListVO> records = page.getRecords().stream()
                .map(order -> toListVO(order,
                        channelMap.get(order.getChannelId()),
                        institutionMap.get(order.getInstId()),
                        productMap.get(order.getProductId()),
                        deductionRecordMap.get(order.getOrderNo())))
                .collect(Collectors.toList());

        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public DeductionRecordSummaryVO summary(DeductionRecordQueryDTO query) {
        DeductionRecordQueryDTO safeQuery = query == null ? new DeductionRecordQueryDTO() : query;

        DeductionRecordSummaryVO summary = new DeductionRecordSummaryVO();
        summary.setTotalCount(applyOrderMapper.selectCount(buildOrderQueryWrapper(safeQuery, false)));
        summary.setDeductedCount(countByOrderStatuses(safeQuery, DEDUCTED_ORDER_STATUSES));
        summary.setUndeductedCount(countByOrderStatuses(safeQuery, Collections.singleton(FAILED_ORDER_STATUS)));
        summary.setSuccessCount(summary.getDeductedCount());
        summary.setFailedCount(summary.getUndeductedCount());
        summary.setAbnormalCount(0L);
        summary.setTotalDeductAmount(sumDeductAmount(safeQuery));
        return summary;
    }

    @Override
    public DeductionRecordListVO detail(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BizException("orderNo is required");
        }
        ApplyOrder order = applyOrderMapper.selectOne(new LambdaQueryWrapper<ApplyOrder>()
                .eq(ApplyOrder::getOrderNo, orderNo.trim())
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException("扣费记录不存在: " + orderNo);
        }

        Channel channel = order.getChannelId() == null ? null : channelMapper.selectById(order.getChannelId());
        Institution institution = order.getInstId() == null ? null : institutionMapper.selectById(order.getInstId());
        InstitutionProduct product = order.getProductId() == null ? null : institutionProductMapper.selectById(order.getProductId());
        DeductionRecord deductionRecord = buildLatestDeductionRecordMap(Collections.singletonList(order.getOrderNo()))
                .get(order.getOrderNo());

        return toListVO(order, channel, institution, product, deductionRecord);
    }

    private LambdaQueryWrapper<ApplyOrder> buildOrderQueryWrapper(DeductionRecordQueryDTO query,
                                                                  boolean includeFinanceStatus) {
        LambdaQueryWrapper<ApplyOrder> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getOrderNo())) {
            wrapper.like(ApplyOrder::getOrderNo, query.getOrderNo().trim());
        }
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.eq(ApplyOrder::getPhoneMd5, DigestUtil.md5Hex(query.getPhone().trim()));
        }
        if (StringUtils.hasText(query.getChannelCode())) {
            wrapper.eq(ApplyOrder::getChannelCode, query.getChannelCode().trim());
        }
        if (StringUtils.hasText(query.getChannelName())) {
            List<Long> channelIds = findChannelIds(query.getChannelName().trim());
            if (channelIds.isEmpty()) {
                wrapper.eq(ApplyOrder::getId, -1L);
            } else {
                wrapper.in(ApplyOrder::getChannelId, channelIds);
            }
        }
        if (StringUtils.hasText(query.getInstCode())) {
            List<Long> instIds = findInstitutionIdsByCode(query.getInstCode().trim());
            if (instIds.isEmpty()) {
                wrapper.eq(ApplyOrder::getId, -1L);
            } else {
                wrapper.in(ApplyOrder::getInstId, instIds);
            }
        }
        if (StringUtils.hasText(query.getInstName())) {
            List<Long> instIds = findInstitutionIdsByName(query.getInstName().trim());
            if (instIds.isEmpty()) {
                wrapper.eq(ApplyOrder::getId, -1L);
            } else {
                wrapper.in(ApplyOrder::getInstId, instIds);
            }
        }

        LocalDateTime startTime = resolveStartTime(query);
        LocalDateTime endTime = resolveEndTime(query);
        if (startTime != null) {
            wrapper.ge(ApplyOrder::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(ApplyOrder::getCreatedAt, endTime);
        }

        if (includeFinanceStatus) {
            Integer financeStatus = resolveFinanceStatus(query);
            if (Integer.valueOf(FINANCE_STATUS_DEDUCTED).equals(financeStatus)) {
                wrapper.in(ApplyOrder::getOrderStatus, DEDUCTED_ORDER_STATUSES);
            } else if (Integer.valueOf(FINANCE_STATUS_UNDEDUCTED).equals(financeStatus)) {
                wrapper.eq(ApplyOrder::getOrderStatus, FAILED_ORDER_STATUS);
            } else {
                wrapper.in(ApplyOrder::getOrderStatus, allVisibleOrderStatuses());
            }
        } else {
            wrapper.in(ApplyOrder::getOrderStatus, allVisibleOrderStatuses());
        }

        return wrapper.orderByDesc(ApplyOrder::getCreatedAt)
                .orderByDesc(ApplyOrder::getUpdatedAt);
    }

    private List<Long> findChannelIds(String keyword) {
        return channelMapper.selectList(new LambdaQueryWrapper<Channel>()
                        .and(w -> w.like(Channel::getChannelName, keyword)
                                .or()
                                .like(Channel::getChannelCode, keyword)))
                .stream()
                .map(Channel::getId)
                .collect(Collectors.toList());
    }

    private List<Long> findInstitutionIdsByCode(String instCode) {
        return institutionMapper.selectList(new LambdaQueryWrapper<Institution>()
                        .eq(Institution::getInstCode, instCode))
                .stream()
                .map(Institution::getId)
                .collect(Collectors.toList());
    }

    private List<Long> findInstitutionIdsByName(String keyword) {
        return institutionMapper.selectList(new LambdaQueryWrapper<Institution>()
                        .and(w -> w.like(Institution::getInstName, keyword)
                                .or()
                                .like(Institution::getMerchantAlias, keyword)
                                .or()
                                .like(Institution::getInstCode, keyword)))
                .stream()
                .map(Institution::getId)
                .collect(Collectors.toList());
    }

    private long countByOrderStatuses(DeductionRecordQueryDTO query, Collection<Integer> orderStatuses) {
        LambdaQueryWrapper<ApplyOrder> wrapper = buildOrderQueryWrapper(query, false);
        wrapper.in(ApplyOrder::getOrderStatus, orderStatuses);
        return applyOrderMapper.selectCount(wrapper);
    }

    private BigDecimal sumDeductAmount(DeductionRecordQueryDTO query) {
        LambdaQueryWrapper<ApplyOrder> wrapper = buildOrderQueryWrapper(query, false)
                .in(ApplyOrder::getOrderStatus, DEDUCTED_ORDER_STATUSES)
                .select(ApplyOrder::getOrderNo, ApplyOrder::getOrderStatus, ApplyOrder::getSettlementPrice);
        List<ApplyOrder> deductedOrders = applyOrderMapper.selectList(wrapper);
        if (deductedOrders.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Map<String, DeductionRecord> deductionRecordMap = buildLatestDeductionRecordMap(deductedOrders.stream()
                .map(ApplyOrder::getOrderNo)
                .collect(Collectors.toList()));
        return deductedOrders.stream()
                .map(order -> resolveDeductAmount(order, deductionRecordMap.get(order.getOrderNo())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Long, Channel> buildChannelMap(List<ApplyOrder> orders) {
        return buildMap(orders.stream().map(ApplyOrder::getChannelId).filter(Objects::nonNull).collect(Collectors.toList()),
                channelMapper::selectBatchIds, Channel::getId);
    }

    private Map<Long, InstitutionProduct> buildProductMap(List<ApplyOrder> orders) {
        return buildMap(orders.stream().map(ApplyOrder::getProductId).filter(Objects::nonNull).collect(Collectors.toList()),
                institutionProductMapper::selectBatchIds, InstitutionProduct::getId);
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
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (distinctOrderNos.isEmpty()) {
            return Collections.emptyMap();
        }

        List<DeductionRecord> deductionRecords = deductionRecordMapper.selectList(new LambdaQueryWrapper<DeductionRecord>()
                .in(DeductionRecord::getOrderNo, distinctOrderNos)
                .eq(DeductionRecord::getStatus, 1)
                .orderByDesc(DeductionRecord::getCreatedAt));

        Map<String, DeductionRecord> latestMap = new LinkedHashMap<>();
        for (DeductionRecord record : deductionRecords) {
            DeductionRecord current = latestMap.get(record.getOrderNo());
            if (current == null || shouldReplaceDeductionRecord(record, current)) {
                latestMap.put(record.getOrderNo(), record);
            }
        }
        return latestMap;
    }

    private boolean shouldReplaceDeductionRecord(DeductionRecord candidate, DeductionRecord current) {
        boolean candidatePushSuccess = Integer.valueOf(3).equals(candidate.getDeductType());
        boolean currentPushSuccess = Integer.valueOf(3).equals(current.getDeductType());
        if (candidatePushSuccess != currentPushSuccess) {
            return candidatePushSuccess;
        }
        if (candidate.getCreatedAt() == null) {
            return false;
        }
        return current.getCreatedAt() == null || candidate.getCreatedAt().isAfter(current.getCreatedAt());
    }

    private DeductionRecordListVO toListVO(ApplyOrder order, Channel channel, Institution institution,
                                           InstitutionProduct product, DeductionRecord deductionRecord) {
        DeductionRecordListVO vo = new DeductionRecordListVO();
        vo.setId(order.getId());
        vo.setDeductionRecordId(deductionRecord == null ? null : deductionRecord.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPhone(decrypt(order.getPhoneEnc(), channel));
        vo.setChannelId(order.getChannelId());
        vo.setChannelCode(order.getChannelCode());
        vo.setChannelName(channel == null ? order.getChannelCode() : channel.getChannelName());
        vo.setInstId(order.getInstId());
        vo.setInstCode(institution == null ? (deductionRecord == null ? null : deductionRecord.getInstCode()) : institution.getInstCode());
        vo.setInstName(institution == null ? null : institution.getInstName());
        vo.setMerchantAlias(institution == null ? null : institution.getMerchantAlias());
        vo.setProductId(order.getProductId());
        vo.setProductName(resolveProductName(order, product));
        vo.setDeductInstitution(resolveDeductInstitution(institution));
        vo.setSettlementMode(channel == null ? null : channel.getSettlementMode());
        vo.setSettlementPrice(order.getSettlementPrice());
        BigDecimal deductAmount = resolveDeductAmount(order, deductionRecord);
        vo.setAmount(deductAmount);
        vo.setDeductAmount(deductAmount);
        int financeStatus = resolveFinanceStatus(order.getOrderStatus());
        vo.setFinanceStatus(financeStatus);
        vo.setFinanceStatusDesc(resolveFinanceStatusDesc(financeStatus));
        vo.setAccountStatus(financeStatus);
        vo.setAccountStatusDesc(resolveFinanceStatusDesc(financeStatus));
        vo.setOrderStatus(order.getOrderStatus());
        vo.setOrderStatusDesc(resolveOrderStatusDesc(order.getOrderStatus()));
        vo.setRejectReason(order.getRejectReason());
        vo.setAbnormalReason(resolveAbnormalReason(order));
        vo.setApplyTime(order.getCreatedAt());
        vo.setDeductTime(financeStatus == FINANCE_STATUS_DEDUCTED
                ? resolveDeductTime(order, deductionRecord)
                : null);
        vo.setCreatedAt(order.getCreatedAt());
        vo.setCreateBy(order.getCreateBy());
        vo.setUpdatedAt(order.getUpdatedAt());
        vo.setUpdateBy(order.getUpdateBy());
        return vo;
    }

    private BigDecimal resolveDeductAmount(ApplyOrder order, DeductionRecord deductionRecord) {
        if (!isDeducted(order.getOrderStatus())) {
            return BigDecimal.ZERO;
        }
        if (deductionRecord != null && deductionRecord.getAmount() != null) {
            return deductionRecord.getAmount();
        }
        return order.getSettlementPrice() == null ? BigDecimal.ZERO : order.getSettlementPrice();
    }

    private LocalDateTime resolveDeductTime(ApplyOrder order, DeductionRecord deductionRecord) {
        if (deductionRecord != null && deductionRecord.getCreatedAt() != null) {
            return deductionRecord.getCreatedAt();
        }
        return order.getUpdatedAt() == null ? order.getCreatedAt() : order.getUpdatedAt();
    }

    private String resolveProductName(ApplyOrder order, InstitutionProduct product) {
        if (product != null && StringUtils.hasText(product.getProductName())) {
            return product.getProductName();
        }
        return order.getProductNameSnapshot();
    }

    private String resolveDeductInstitution(Institution institution) {
        if (institution == null) {
            return null;
        }
        if (StringUtils.hasText(institution.getMerchantAlias())) {
            return institution.getMerchantAlias();
        }
        return institution.getInstName();
    }

    private String resolveAbnormalReason(ApplyOrder order) {
        if (Integer.valueOf(FAILED_ORDER_STATUS).equals(order.getOrderStatus())) {
            return StringUtils.hasText(order.getRejectReason()) ? order.getRejectReason() : "进件失败";
        }
        return null;
    }

    private boolean isDeducted(Integer orderStatus) {
        return orderStatus != null && DEDUCTED_ORDER_STATUSES.contains(orderStatus);
    }

    private int resolveFinanceStatus(Integer orderStatus) {
        return isDeducted(orderStatus) ? FINANCE_STATUS_DEDUCTED : FINANCE_STATUS_UNDEDUCTED;
    }

    private String resolveFinanceStatusDesc(int financeStatus) {
        return financeStatus == FINANCE_STATUS_DEDUCTED ? "已扣减" : "未扣减";
    }

    private String resolveOrderStatusDesc(Integer orderStatus) {
        if (orderStatus == null) {
            return "-";
        }
        return switch (orderStatus) {
            case 0 -> "待处理";
            case 1 -> "进件成功";
            case 2 -> "授信通过";
            case 3 -> "已放款";
            case 9 -> "进件失败";
            default -> String.valueOf(orderStatus);
        };
    }

    private Integer resolveFinanceStatus(DeductionRecordQueryDTO query) {
        return query.getFinanceStatus() == null ? query.getAccountStatus() : query.getFinanceStatus();
    }

    private Collection<Integer> allVisibleOrderStatuses() {
        return List.of(1, 2, 3, FAILED_ORDER_STATUS);
    }

    private LocalDateTime resolveStartTime(DeductionRecordQueryDTO query) {
        return query.getDeductStartTime() == null ? query.getStartTime() : query.getDeductStartTime();
    }

    private LocalDateTime resolveEndTime(DeductionRecordQueryDTO query) {
        return query.getDeductEndTime() == null ? query.getEndTime() : query.getDeductEndTime();
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

    private String decrypt(String cipherText, Channel channel) {
        if (!StringUtils.hasText(cipherText) || channel == null || !StringUtils.hasText(channel.getAppKey())) {
            return null;
        }
        try {
            return AesUtil.decrypt(cipherText, channel.getAppKey());
        } catch (Exception ex) {
            log.warn("[ADMIN-DEDUCTION] decrypt failed, channelCode={}", channel.getChannelCode(), ex);
            return null;
        }
    }
}
