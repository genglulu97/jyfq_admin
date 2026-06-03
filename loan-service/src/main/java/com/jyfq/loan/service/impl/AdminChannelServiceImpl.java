package com.jyfq.loan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.common.util.AuditOperatorUtil;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.model.dto.ChannelQueryDTO;
import com.jyfq.loan.model.dto.ChannelSaveDTO;
import com.jyfq.loan.model.entity.ApplyOrder;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.vo.ChannelListVO;
import com.jyfq.loan.service.AdminChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Admin channel management service implementation.
 */
@Service
@RequiredArgsConstructor
public class AdminChannelServiceImpl implements AdminChannelService {

    private static final String PRICE_RETURN_MODE_BEFORE_PROFIT = "BEFORE_PROFIT";
    private static final String PRICE_RETURN_MODE_AFTER_PROFIT = "AFTER_PROFIT";

    private final ChannelMapper channelMapper;
    private final ApplyOrderMapper applyOrderMapper;

    @Override
    public PageResult<ChannelListVO> pageChannels(ChannelQueryDTO query) {
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10L : Math.min(query.getSize(), 100L);

        LambdaQueryWrapper<Channel> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getChannelName())) {
            wrapper.like(Channel::getChannelName, query.getChannelName().trim());
        }
        if (StringUtils.hasText(query.getChannelCode())) {
            wrapper.like(Channel::getChannelCode, query.getChannelCode().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Channel::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Channel::getCreatedAt).orderByDesc(Channel::getId);

        Page<Channel> page = channelMapper.selectPage(new Page<>(current, size), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResult.empty(current, size);
        }

        Map<Long, Integer> todayApplyCounts = countTodayApplyOrders(page.getRecords());
        List<ChannelListVO> records = page.getRecords().stream()
                .map(channel -> {
                    ChannelListVO vo = toListVO(channel);
                    vo.setActualPushCount(todayApplyCounts.getOrDefault(channel.getId(), 0));
                    return vo;
                })
                .collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public ChannelListVO detail(Long id) {
        Channel channel = channelMapper.selectById(id);
        if (channel == null) {
            throw new BizException("Channel not found: " + id);
        }
        ChannelListVO vo = toListVO(channel);
        vo.setActualPushCount(countTodayApplyOrders(Collections.singletonList(channel)).getOrDefault(channel.getId(), 0));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createChannel(ChannelSaveDTO request) {
        ensureChannelIdUnique(null, request.getChannelId());
        ensureChannelCodeUnique(null, request.getChannelCode());
        Channel channel = new Channel();
        fillChannel(channel, request);
        channelMapper.insert(channel);
        return channel.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChannel(Long id, ChannelSaveDTO request) {
        Channel existing = channelMapper.selectById(id);
        if (existing == null) {
            throw new BizException("渠道不存在: " + id);
        }
        ensureChannelIdUnique(id, request.getChannelId());
        ensureChannelCodeUnique(id, request.getChannelCode());

        Channel channel = new Channel();
        channel.setId(id);
        fillChannel(channel, request);
        channelMapper.updateById(channel);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleChannel(Long id) {
        Channel existing = channelMapper.selectById(id);
        if (existing == null) {
            throw new BizException("渠道不存在: " + id);
        }
        int targetStatus = Integer.valueOf(1).equals(existing.getStatus()) ? 0 : 1;
        channelMapper.update(null, new LambdaUpdateWrapper<Channel>()
                .eq(Channel::getId, id)
                .set(Channel::getStatus, targetStatus)
                .set(Channel::getUpdateBy, AuditOperatorUtil.currentOperator()));
    }

    private void fillChannel(Channel channel, ChannelSaveDTO request) {
        validatePriceRange(request.getMinPrice(), request.getMaxPrice());
        channel.setChannelId(request.getChannelId().trim());
        channel.setChannelName(request.getChannelName().trim());
        channel.setChannelCode(request.getChannelCode().trim());
        channel.setChannelType(request.getChannelType().trim());
        channel.setH5Url(resolveH5Url(request));
        channel.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        channel.setBusinessOwner(trimToNull(request.getBusinessOwner()));
        channel.setDailyQuota(defaultInt(request.getDailyQuota(), 10000));
        channel.setNormalRecommend(defaultInt(request.getNormalRecommend(), 0));
        channel.setDisplayProductCount(defaultInt(request.getDisplayProductCount(), 1));
        channel.setActualPushCount(defaultInt(request.getActualPushCount(), 1));
        channel.setMethodName(trimToNull(request.getMethodName()));
        channel.setEncryptType(defaultText(request.getEncryptType(), "AES"));
        channel.setCipherMode(defaultText(request.getCipherMode(), "ECB"));
        channel.setPaddingMode(defaultText(request.getPaddingMode(), "PKCS5Padding"));
        channel.setIvValue(trimToNull(request.getIvValue()));
        channel.setAppKey(request.getAppKey().trim());
        channel.setIpWhitelist(trimToNull(request.getIpWhitelist()));
        channel.setCallbackUrl(trimToNull(request.getCallbackUrl()));
        channel.setSettlementMode(defaultText(request.getSettlementMode(), "CPA"));
        channel.setFeeRate(request.getFeeRate());
        channel.setMinPrice(request.getMinPrice());
        channel.setMaxPrice(request.getMaxPrice());
        channel.setPriceReturnMode(resolvePriceReturnMode(request.getPriceReturnMode()));
        channel.setExtJson(trimToNull(request.getExtJson()));
        channel.setRemark(trimToNull(request.getRemark()));
    }

    private void ensureChannelIdUnique(Long id, String channelId) {
        LambdaQueryWrapper<Channel> wrapper = new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelId, channelId.trim());
        if (id != null) {
            wrapper.ne(Channel::getId, id);
        }
        if (channelMapper.selectCount(wrapper) > 0) {
            throw new BizException("渠道ID已存在: " + channelId);
        }
    }

    private void ensureChannelCodeUnique(Long id, String channelCode) {
        LambdaQueryWrapper<Channel> wrapper = new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelCode, channelCode.trim());
        if (id != null) {
            wrapper.ne(Channel::getId, id);
        }
        if (channelMapper.selectCount(wrapper) > 0) {
            throw new BizException("渠道标识已存在: " + channelCode);
        }
    }

    private ChannelListVO toListVO(Channel channel) {
        ChannelListVO vo = new ChannelListVO();
        vo.setId(channel.getId());
        vo.setChannelId(channel.getChannelId());
        vo.setChannelName(channel.getChannelName());
        vo.setChannelCode(channel.getChannelCode());
        vo.setChannelType(channel.getChannelType());
        vo.setH5Url(channel.getH5Url());
        vo.setStatus(channel.getStatus());
        vo.setStatusDesc(Integer.valueOf(1).equals(channel.getStatus()) ? "启用" : "禁用");
        vo.setBusinessOwner(channel.getBusinessOwner());
        vo.setDailyQuota(channel.getDailyQuota());
        vo.setNormalRecommend(channel.getNormalRecommend());
        vo.setDisplayProductCount(channel.getDisplayProductCount());
        vo.setActualPushCount(channel.getActualPushCount());
        vo.setMethodName(channel.getMethodName());
        vo.setEncryptType(channel.getEncryptType());
        vo.setCipherMode(channel.getCipherMode());
        vo.setPaddingMode(channel.getPaddingMode());
        vo.setIvValue(channel.getIvValue());
        vo.setAppKey(channel.getAppKey());
        vo.setIpWhitelist(channel.getIpWhitelist());
        vo.setCallbackUrl(channel.getCallbackUrl());
        vo.setSettlementMode(channel.getSettlementMode());
        vo.setFeeRate(channel.getFeeRate());
        vo.setMinPrice(channel.getMinPrice());
        vo.setMaxPrice(channel.getMaxPrice());
        vo.setPriceReturnMode(channel.getPriceReturnMode());
        vo.setExtJson(channel.getExtJson());
        vo.setRemark(channel.getRemark());
        vo.setCreatedAt(channel.getCreatedAt());
        vo.setCreateBy(channel.getCreateBy());
        vo.setUpdatedAt(channel.getUpdatedAt());
        vo.setUpdateBy(channel.getUpdateBy());
        return vo;
    }

    private Map<Long, Integer> countTodayApplyOrders(List<Channel> channels) {
        List<Long> channelIds = channels.stream()
                .map(Channel::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (channelIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
        List<Map<String, Object>> rows = applyOrderMapper.selectMaps(new QueryWrapper<ApplyOrder>()
                .select("channel_id AS channelId", "COUNT(1) AS total")
                .in("channel_id", channelIds)
                .ge("created_at", startOfToday)
                .lt("created_at", startOfTomorrow)
                .groupBy("channel_id"));

        Map<Long, Integer> counts = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long channelId = toLong(firstPresent(row, "channelId", "channel_id", "CHANNELID", "CHANNEL_ID"));
            Integer total = toInteger(firstPresent(row, "total", "TOTAL"));
            if (channelId != null && total != null) {
                counts.put(channelId, total);
            }
        }
        return counts;
    }

    private Object firstPresent(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            return Long.parseLong(value.toString());
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        return null;
    }

    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BizException("minPrice cannot be greater than maxPrice");
        }
    }

    private String resolvePriceReturnMode(String value) {
        if (!StringUtils.hasText(value)) {
            return PRICE_RETURN_MODE_BEFORE_PROFIT;
        }
        String trimmed = value.trim();
        String normalized = trimmed.toUpperCase();
        if (PRICE_RETURN_MODE_BEFORE_PROFIT.equals(normalized)
                || "BEFORE".equals(normalized)
                || "PRE".equals(normalized)
                || "0".equals(normalized)
                || "\u5206\u6da6\u524d".equals(trimmed)
                || "\u5206\u6da6\u524d\u4ef7\u683c".equals(trimmed)) {
            return PRICE_RETURN_MODE_BEFORE_PROFIT;
        }
        if (PRICE_RETURN_MODE_AFTER_PROFIT.equals(normalized)
                || "AFTER".equals(normalized)
                || "POST".equals(normalized)
                || "1".equals(normalized)
                || "\u5206\u6da6\u540e".equals(trimmed)
                || "\u5206\u6da6\u540e\u4ef7\u683c".equals(trimmed)) {
            return PRICE_RETURN_MODE_AFTER_PROFIT;
        }
        throw new BizException("priceReturnMode must be BEFORE_PROFIT or AFTER_PROFIT");
    }

    private String resolveH5Url(ChannelSaveDTO request) {
        if (!"H5".equalsIgnoreCase(request.getChannelType().trim())) {
            return null;
        }
        String h5Url = trimToNull(request.getH5Url());
        if (h5Url == null) {
            throw new BizException("H5链接不能为空");
        }
        if (h5Url.length() > 1024) {
            throw new BizException("H5链接长度不能超过1024");
        }
        return h5Url;
    }
}
