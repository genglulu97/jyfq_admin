package com.jyfq.loan.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.util.AesUtil;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.mapper.InstitutionMapper;
import com.jyfq.loan.mapper.InstitutionProductMapper;
import com.jyfq.loan.mapper.PushRecordMapper;
import com.jyfq.loan.model.dto.OrderQueryDTO;
import com.jyfq.loan.model.entity.ApplyOrder;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.entity.Institution;
import com.jyfq.loan.model.entity.InstitutionProduct;
import com.jyfq.loan.model.entity.PushRecord;
import com.jyfq.loan.model.enums.OrderStatus;
import com.jyfq.loan.model.enums.PushStatus;
import com.jyfq.loan.model.vo.OrderDetailVO;
import com.jyfq.loan.model.vo.OrderListVO;
import com.jyfq.loan.model.vo.OrderPushRecordVO;
import com.jyfq.loan.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin order query service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final ApplyOrderMapper applyOrderMapper;
    private final ChannelMapper channelMapper;
    private final InstitutionMapper institutionMapper;
    private final InstitutionProductMapper institutionProductMapper;
    private final PushRecordMapper pushRecordMapper;

    @Override
    public PageResult<OrderListVO> pageOrders(OrderQueryDTO query) {
        long current = normalizePageNo(query.getCurrent());
        long size = normalizePageSize(query.getSize());

        LambdaQueryWrapper<ApplyOrder> wrapper = buildOrderQueryWrapper(query);
        Page<ApplyOrder> page = applyOrderMapper.selectPage(new Page<>(current, size), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResult.empty(current, size);
        }

        Map<Long, Channel> channelMap = buildChannelMap(page.getRecords());
        Map<Long, InstitutionProduct> productMap = buildProductMap(page.getRecords());
        Map<String, PushRecord> latestPushRecordMap = buildLatestPushRecordMap(
                page.getRecords().stream().map(ApplyOrder::getOrderNo).collect(Collectors.toList()));

        List<OrderListVO> records = page.getRecords().stream()
                .map(order -> toOrderListVO(order, channelMap.get(order.getChannelId()),
                        productMap.get(order.getProductId()), latestPushRecordMap.get(order.getOrderNo())))
                .collect(Collectors.toList());

        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public OrderDetailVO getOrderDetail(String orderNo) {
        ApplyOrder order = applyOrderMapper.selectOne(new LambdaQueryWrapper<ApplyOrder>()
                .eq(ApplyOrder::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException("订单不存在: " + orderNo);
        }

        Channel channel = order.getChannelId() == null ? null : channelMapper.selectById(order.getChannelId());
        InstitutionProduct product = order.getProductId() == null ? null : institutionProductMapper.selectById(order.getProductId());
        Institution institution = order.getInstId() == null ? null : institutionMapper.selectById(order.getInstId());
        PushRecord latestPushRecord = buildLatestPushRecordMap(Collections.singletonList(orderNo)).get(orderNo);

        return toOrderDetailVO(order, channel, institution, product, latestPushRecord);
    }

    @Override
    public List<OrderPushRecordVO> listPushRecords(String orderNo) {
        List<PushRecord> pushRecords = pushRecordMapper.selectList(new LambdaQueryWrapper<PushRecord>()
                .eq(PushRecord::getOrderNo, orderNo)
                .orderByDesc(PushRecord::getPushedAt)
                .orderByDesc(PushRecord::getCreatedAt));
        if (pushRecords.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Institution> institutionMap = buildInstitutionMap(
                pushRecords.stream().map(PushRecord::getInstId).filter(Objects::nonNull).collect(Collectors.toList()));
        Map<Long, InstitutionProduct> productMap = buildInstitutionProductMap(
                pushRecords.stream().map(PushRecord::getProductId).filter(Objects::nonNull).collect(Collectors.toList()));

        return pushRecords.stream().map(record -> {
            OrderPushRecordVO vo = new OrderPushRecordVO();
            vo.setId(record.getId());
            vo.setOrderNo(record.getOrderNo());
            vo.setInstCode(record.getInstCode());
            Institution institution = institutionMap.get(record.getInstId());
            vo.setInstName(institution == null ? null : institution.getInstName());
            InstitutionProduct product = productMap.get(record.getProductId());
            vo.setProductName(product == null ? null : product.getProductName());
            vo.setRequestId(record.getRequestId());
            vo.setThirdOrderNo(record.getThirdOrderNo());
            vo.setPushStatus(record.getPushStatus());
            vo.setPushStatusDesc(resolvePushStatusDesc(record.getPushStatus()));
            vo.setErrorMsg(record.getErrorMsg());
            vo.setCostMs(record.getCostMs());
            vo.setPushedAt(record.getPushedAt());
            vo.setNotifyAt(record.getNotifyAt());
            vo.setCreatedAt(record.getCreatedAt());
            vo.setCreateBy(record.getCreateBy());
            vo.setUpdatedAt(record.getUpdatedAt());
            vo.setUpdateBy(record.getUpdateBy());
            return vo;
        }).collect(Collectors.toList());
    }

    private LambdaQueryWrapper<ApplyOrder> buildOrderQueryWrapper(OrderQueryDTO query) {
        LambdaQueryWrapper<ApplyOrder> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getPhone())) {
            wrapper.eq(ApplyOrder::getPhoneMd5, DigestUtil.md5Hex(query.getPhone().trim()));
        }
        if (StringUtils.hasText(query.getUserName())) {
            wrapper.eq(ApplyOrder::getUserNameMd5, DigestUtil.md5Hex(query.getUserName().trim()));
        }
        if (StringUtils.hasText(query.getChannelCode())) {
            wrapper.eq(ApplyOrder::getChannelCode, query.getChannelCode().trim());
        }
        if (StringUtils.hasText(query.getCustomerLevel())) {
            wrapper.eq(ApplyOrder::getCustomerLevel, query.getCustomerLevel().trim());
        }
        if (StringUtils.hasText(query.getCityKeyword())) {
            String cityKeyword = query.getCityKeyword().trim();
            wrapper.and(w -> w.eq(ApplyOrder::getCityCode, cityKeyword)
                    .or()
                    .like(ApplyOrder::getWorkCity, cityKeyword));
        }
        if (query.getOrderStatus() != null) {
            wrapper.eq(ApplyOrder::getOrderStatus, query.getOrderStatus());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(ApplyOrder::getCreatedAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(ApplyOrder::getCreatedAt, query.getEndTime());
        }
        if (StringUtils.hasText(query.getMerchantAlias())) {
            List<Long> productIds = institutionProductMapper.selectList(new LambdaQueryWrapper<InstitutionProduct>()
                            .like(InstitutionProduct::getProductName, query.getMerchantAlias().trim()))
                    .stream()
                    .map(InstitutionProduct::getId)
                    .collect(Collectors.toList());
            if (productIds.isEmpty()) {
                wrapper.isNull(ApplyOrder::getId);
            } else {
                wrapper.in(ApplyOrder::getProductId, productIds);
            }
        }

        return wrapper.orderByDesc(ApplyOrder::getCreatedAt);
    }

    private Map<Long, Channel> buildChannelMap(List<ApplyOrder> orders) {
        return buildMap(orders.stream().map(ApplyOrder::getChannelId).filter(Objects::nonNull).collect(Collectors.toList()),
                channelMapper::selectBatchIds, Channel::getId);
    }

    private Map<Long, InstitutionProduct> buildProductMap(List<ApplyOrder> orders) {
        return buildInstitutionProductMap(orders.stream().map(ApplyOrder::getProductId).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    private Map<Long, InstitutionProduct> buildInstitutionProductMap(List<Long> productIds) {
        return buildMap(productIds, institutionProductMapper::selectBatchIds, InstitutionProduct::getId);
    }

    private Map<Long, Institution> buildInstitutionMap(List<Long> instIds) {
        return buildMap(instIds, institutionMapper::selectBatchIds, Institution::getId);
    }

    private <K, T> Map<K, T> buildMap(List<K> ids, Function<Collection<K>, List<T>> query, Function<T, K> keyMapper) {
        List<K> distinctIds = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return query.apply(distinctIds).stream()
                .collect(Collectors.toMap(keyMapper, Function.identity(), (left, right) -> left));
    }

    private Map<String, PushRecord> buildLatestPushRecordMap(List<String> orderNos) {
        List<String> distinctOrderNos = orderNos.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (distinctOrderNos.isEmpty()) {
            return Collections.emptyMap();
        }

        List<PushRecord> pushRecords = pushRecordMapper.selectList(new LambdaQueryWrapper<PushRecord>()
                .in(PushRecord::getOrderNo, distinctOrderNos)
                .orderByDesc(PushRecord::getPushedAt)
                .orderByDesc(PushRecord::getCreatedAt));

        Map<String, PushRecord> latestPushMap = new java.util.LinkedHashMap<>();
        for (PushRecord pushRecord : pushRecords) {
            latestPushMap.putIfAbsent(pushRecord.getOrderNo(), pushRecord);
        }
        return latestPushMap;
    }

    private String resolveProductSnapshot(ApplyOrder order, InstitutionProduct product, PushRecord latestPushRecord) {
        if (StringUtils.hasText(order.getProductNameSnapshot())) {
            return order.getProductNameSnapshot();
        }
        if (product != null && StringUtils.hasText(product.getProductName())) {
            return product.getProductName();
        }
        return latestPushRecord == null ? null : latestPushRecord.getInstCode();
    }

    private OrderListVO toOrderListVO(ApplyOrder order, Channel channel,
                                      InstitutionProduct product, PushRecord latestPushRecord) {
        OrderListVO vo = new OrderListVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserName(decrypt(order.getUserName(), channel));
        vo.setPhone(decrypt(order.getPhoneEnc(), channel));
        vo.setChannelCode(order.getChannelCode());
        vo.setChannelName(channel == null ? order.getChannelCode() : channel.getChannelName());
        vo.setCityName(resolveCityName(order));
        vo.setMerchantAlias(resolveProductSnapshot(order, product, latestPushRecord));
        vo.setLoanAmount(order.getLoanAmount());
        vo.setLoanAmountRange(formatLoanAmountRange(order.getLoanAmount()));
        vo.setCustomerLevel(resolveCustomerLevel(order));
        vo.setOrderStatus(order.getOrderStatus());
        vo.setOrderStatusDesc(resolveOrderStatusDesc(order.getOrderStatus()));
        vo.setFollowSalesman(StringUtils.hasText(order.getFollowSalesman()) ? order.getFollowSalesman() : "-");
        vo.setCreatedAt(order.getCreatedAt());
        vo.setCreateBy(order.getCreateBy());
        vo.setUpdatedAt(order.getUpdatedAt());
        vo.setUpdateBy(order.getUpdateBy());
        if (latestPushRecord != null && !StringUtils.hasText(vo.getMerchantAlias())) {
            vo.setMerchantAlias(latestPushRecord.getInstCode());
        }
        return vo;
    }

    private OrderDetailVO toOrderDetailVO(ApplyOrder order, Channel channel, Institution institution,
                                          InstitutionProduct product, PushRecord latestPushRecord) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setChannelCode(order.getChannelCode());
        vo.setChannelName(channel == null ? order.getChannelCode() : channel.getChannelName());
        vo.setUserName(decrypt(order.getUserName(), channel));
        vo.setPhone(decrypt(order.getPhoneEnc(), channel));
        vo.setIdCard(decrypt(order.getIdCardEnc(), channel));
        vo.setGenderDesc(resolveGender(order.getGender()));
        vo.setWorkCity(resolveCityName(order));
        vo.setAge(order.getAge());
        vo.setProfessionDesc(resolveProfession(order.getProfession()));
        vo.setProvidentFundDesc(resolveBinaryStatus(order.getProvidentFund()));
        vo.setSocialSecurityDesc(resolveBinaryStatus(order.getSocialSecurity()));
        vo.setZhimaDesc(resolveZhimaDesc(order.getZhima()));
        vo.setOverdueDesc(resolveOverdueStatus(order.getOverdue()));
        vo.setHouseDesc(resolveBinaryStatus(order.getHouse()));
        vo.setVehicleDesc(resolveBinaryStatus(order.getVehicle()));
        vo.setVehicleStatus(StringUtils.hasText(order.getVehicleStatus()) ? order.getVehicleStatus() : "未知");
        vo.setVehicleValue(StringUtils.hasText(order.getVehicleValue()) ? order.getVehicleValue() : "未知");
        vo.setInsuranceDesc(resolveBinaryStatus(order.getCommercialInsurance()));
        vo.setDeviceIp(order.getDeviceIp());
        vo.setLoanAmount(order.getLoanAmount());
        vo.setLoanAmountRange(formatLoanAmountRange(order.getLoanAmount()));
        vo.setCustomerLevel(resolveCustomerLevel(order));
        vo.setSettlementPrice(order.getSettlementPrice());
        vo.setMerchantName(institution == null ? null : institution.getInstName());
        vo.setMerchantAlias(resolveProductSnapshot(order, product, latestPushRecord));
        vo.setPushStatusDesc(latestPushRecord == null ? "-" : resolvePushStatusDesc(latestPushRecord.getPushStatus()));
        vo.setOrderStatusDesc(resolveOrderStatusDesc(order.getOrderStatus()));
        vo.setFollowSalesman(StringUtils.hasText(order.getFollowSalesman()) ? order.getFollowSalesman() : "-");
        vo.setSalesmanRating(order.getSalesmanRating());
        vo.setFollowRemark(StringUtils.hasText(order.getFollowRemark()) ? order.getFollowRemark() : "暂无跟进备注");
        vo.setAllocationTime(order.getAllocationTime());
        vo.setFinalLoanAmount(order.getFinalLoanAmount());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setCreateBy(order.getCreateBy());
        vo.setUpdatedAt(order.getUpdatedAt());
        vo.setUpdateBy(order.getUpdateBy());
        return vo;
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
            log.warn("[ADMIN-ORDER] decrypt failed, channelCode={}", channel.getChannelCode(), ex);
            return null;
        }
    }

    private String resolveCityName(ApplyOrder order) {
        if (StringUtils.hasText(order.getWorkCity())) {
            return order.getWorkCity();
        }
        return order.getCityCode();
    }

    private String resolveOrderStatusDesc(Integer orderStatus) {
        if (orderStatus == null) {
            return "-";
        }
        try {
            return OrderStatus.of(orderStatus).getDesc();
        } catch (Exception ex) {
            return String.valueOf(orderStatus);
        }
    }

    private String resolvePushStatusDesc(Integer pushStatus) {
        if (pushStatus == null) {
            return "-";
        }
        try {
            return PushStatus.of(pushStatus).getDesc();
        } catch (Exception ex) {
            return String.valueOf(pushStatus);
        }
    }

    private String resolveCustomerLevel(ApplyOrder order) {
        if (StringUtils.hasText(order.getCustomerLevel())) {
            return order.getCustomerLevel();
        }

        List<Integer> indicators = new ArrayList<>();
        indicators.add(order.getHouse());
        indicators.add(order.getVehicle());
        indicators.add(order.getProvidentFund());
        indicators.add(order.getSocialSecurity());
        indicators.add(order.getCommercialInsurance());
        if (indicators.stream().allMatch(Objects::isNull) && order.getZhima() == null && order.getOverdue() == null) {
            return "-";
        }

        int score = 0;
        if (Objects.equals(order.getHouse(), 1)) {
            score += 2;
        }
        if (Objects.equals(order.getVehicle(), 1)) {
            score += 2;
        }
        if (Objects.equals(order.getProvidentFund(), 1)) {
            score += 1;
        }
        if (Objects.equals(order.getSocialSecurity(), 1)) {
            score += 1;
        }
        if (Objects.equals(order.getCommercialInsurance(), 1)) {
            score += 1;
        }
        if (order.getZhima() != null) {
            if (order.getZhima() >= 700) {
                score += 2;
            } else if (order.getZhima() >= 650) {
                score += 1;
            }
        }
        if (Objects.equals(order.getOverdue(), 1)) {
            score += 1;
        }

        int stars = Math.max(1, Math.min(5, (score + 1) / 2));
        return stars + "星";
    }

    private String resolveGender(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        };
    }

    private String resolveProfession(Integer profession) {
        if (profession == null) {
            return "未知";
        }
        return switch (profession) {
            case 1 -> "上班族";
            case 2 -> "自由职业";
            case 3 -> "企业主";
            case 4 -> "公职人员";
            default -> "未知";
        };
    }

    private String resolveBinaryStatus(Integer value) {
        if (value == null) {
            return "未知";
        }
        return switch (value) {
            case 1 -> "有";
            case 2 -> "无";
            default -> "未知";
        };
    }

    private String resolveOverdueStatus(Integer overdue) {
        if (overdue == null) {
            return "未知";
        }
        return switch (overdue) {
            case 1 -> "无";
            case 2 -> "有";
            default -> "未知";
        };
    }

    private String resolveZhimaDesc(Integer zhima) {
        if (zhima == null) {
            return "未知";
        }
        if (zhima < 600) {
            return "600以下";
        }
        if (zhima < 650) {
            return "600-649";
        }
        if (zhima < 700) {
            return "650-699";
        }
        return "700以上";
    }

    private String formatLoanAmountRange(Integer loanAmount) {
        if (loanAmount == null || loanAmount <= 0) {
            return "-";
        }
        if (loanAmount < 10000) {
            return "1万以下";
        }
        if (loanAmount < 50000) {
            return "1-5万";
        }
        if (loanAmount < 100000) {
            return "5-10万";
        }
        if (loanAmount < 200000) {
            return "10-20万";
        }
        return "20万以上";
    }
}
