package com.jyfq.loan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.mapper.H5PromotionEventMapper;
import com.jyfq.loan.model.dto.H5PromotionQueryDTO;
import com.jyfq.loan.model.dto.H5TrackDTO;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.entity.H5PromotionEvent;
import com.jyfq.loan.model.vo.H5PromotionListVO;
import com.jyfq.loan.model.vo.H5PromotionSummaryVO;
import com.jyfq.loan.service.H5PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * H5 promotion statistics.
 */
@Service
@RequiredArgsConstructor
public class H5PromotionServiceImpl implements H5PromotionService {

    private static final String CHANNEL_TYPE_H5 = "H5";
    private static final String EVENT_PV = "PV";
    private static final String EVENT_CLICK = "CLICK";
    private static final String EVENT_REGISTER = "REGISTER";
    private static final String EVENT_COMPLETE = "COMPLETE";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChannelMapper channelMapper;
    private final H5PromotionEventMapper eventMapper;

    @Override
    public PageResult<H5PromotionListVO> pagePromotions(H5PromotionQueryDTO query) {
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 10L : Math.min(query.getSize(), 100L);
        LambdaQueryWrapper<Channel> wrapper = buildChannelWrapper(query);
        wrapper.orderByDesc(Channel::getUpdatedAt).orderByDesc(Channel::getId);

        Page<Channel> page = channelMapper.selectPage(new Page<>(current, size), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResult.empty(current, size);
        }

        Map<Long, Metrics> metricsMap = aggregateMetrics(extractIds(page.getRecords()), query);
        List<H5PromotionListVO> records = page.getRecords().stream()
                .map(channel -> toListVO(channel, metricsMap.getOrDefault(channel.getId(), Metrics.empty())))
                .collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public H5PromotionSummaryVO summary(H5PromotionQueryDTO query) {
        List<Channel> channels = channelMapper.selectList(buildChannelWrapper(query));
        Metrics metrics = aggregateTotalMetrics(extractIds(channels), query);

        H5PromotionSummaryVO vo = new H5PromotionSummaryVO();
        vo.setChannelCount((long) channels.size());
        vo.setPvCount(metrics.pvCount());
        vo.setClickCount(metrics.clickCount());
        vo.setRegisterCount(metrics.registerCount());
        vo.setCompleteCount(metrics.completeCount());
        vo.setClickRate(rate(metrics.clickCount(), metrics.pvCount()));
        vo.setRegisterConversionRate(rate(metrics.registerCount(), metrics.clickCount()));
        vo.setCompleteConversionRate(rate(metrics.completeCount(), metrics.registerCount()));
        return vo;
    }

    @Override
    public void track(H5TrackDTO request, String clientIp, String userAgent, String referer) {
        String eventType = normalizeEventType(request.getEventType());
        Channel channel = findH5Channel(request.getChannelCode());
        String deviceIp = limit(trimToNull(clientIp), 45);

        if (shouldDedupeByIp(eventType) && hasTrackedToday(channel.getId(), eventType, deviceIp)) {
            return;
        }

        H5PromotionEvent event = new H5PromotionEvent();
        event.setChannelId(channel.getId());
        event.setChannelCode(channel.getChannelCode());
        event.setEventType(eventType);
        event.setVisitorId(trimToNull(request.getVisitorId()));
        event.setSessionId(trimToNull(request.getSessionId()));
        event.setPageUrl(limit(trimToNull(request.getPageUrl()), 1024));
        event.setReferer(limit(firstText(request.getReferer(), referer), 1024));
        event.setUserAgent(limit(trimToNull(userAgent), 512));
        event.setDeviceIp(deviceIp);
        event.setExtJson(trimToNull(request.getExtJson()));
        eventMapper.insert(event);
    }

    @Override
    public void trackComplete(String channelCode, String clientIp, String userAgent, String referer) {
        if (!StringUtils.hasText(channelCode)) {
            return;
        }
        try {
            H5TrackDTO request = new H5TrackDTO();
            request.setChannelCode(channelCode);
            request.setEventType(EVENT_COMPLETE);
            track(request, clientIp, userAgent, referer);
        } catch (Exception ignored) {
            // Tracking must not block a successful H5 application submit.
        }
    }

    private LambdaQueryWrapper<Channel> buildChannelWrapper(H5PromotionQueryDTO query) {
        LambdaQueryWrapper<Channel> wrapper = new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelType, CHANNEL_TYPE_H5);
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Channel::getChannelName, keyword)
                    .or()
                    .like(Channel::getChannelCode, keyword));
        }
        if (StringUtils.hasText(query.getLinkKeyword())) {
            wrapper.like(Channel::getH5Url, query.getLinkKeyword().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Channel::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private H5PromotionListVO toListVO(Channel channel, Metrics metrics) {
        H5PromotionListVO vo = new H5PromotionListVO();
        vo.setId(channel.getId());
        vo.setChannelName(channel.getChannelName());
        vo.setChannelCode(channel.getChannelCode());
        vo.setPromoteLink(channel.getH5Url());
        vo.setStatus(channel.getStatus());
        vo.setStatusDesc(Integer.valueOf(1).equals(channel.getStatus()) ? "enabled" : "disabled");
        vo.setPvCount(metrics.pvCount());
        vo.setClickCount(metrics.clickCount());
        vo.setClickRate(rate(metrics.clickCount(), metrics.pvCount()));
        vo.setRegisterCount(metrics.registerCount());
        vo.setCompleteCount(metrics.completeCount());
        vo.setRegisterConversionRate(rate(metrics.registerCount(), metrics.clickCount()));
        vo.setCompleteConversionRate(rate(metrics.completeCount(), metrics.registerCount()));
        vo.setUpdatedAt(channel.getUpdatedAt());
        return vo;
    }

    private Map<Long, Metrics> aggregateMetrics(List<Long> channelIds, H5PromotionQueryDTO query) {
        if (channelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<H5PromotionEvent> wrapper = buildEventAggregateWrapper(query)
                .select("channel_id AS channelId",
                        "COUNT(DISTINCT CASE WHEN event_type = 'PV' AND device_ip IS NOT NULL AND device_ip <> '' THEN CONCAT(DATE(created_at), '#', device_ip) END) AS pvCount",
                        "COUNT(DISTINCT CASE WHEN event_type = 'CLICK' AND device_ip IS NOT NULL AND device_ip <> '' THEN CONCAT(DATE(created_at), '#', device_ip) END) AS clickCount",
                        "COALESCE(SUM(CASE WHEN event_type = 'REGISTER' THEN 1 ELSE 0 END), 0) AS registerCount",
                        "COALESCE(SUM(CASE WHEN event_type = 'COMPLETE' THEN 1 ELSE 0 END), 0) AS completeCount")
                .in("channel_id", channelIds)
                .groupBy("channel_id");

        Map<Long, Metrics> result = new HashMap<>();
        for (Map<String, Object> row : eventMapper.selectMaps(wrapper)) {
            if (row == null) {
                continue;
            }
            Long channelId = toLong(firstPresent(row, "channelId", "channel_id", "CHANNELID", "CHANNEL_ID"));
            if (channelId != null) {
                result.put(channelId, metricsFrom(row));
            }
        }
        return result;
    }

    private Metrics aggregateTotalMetrics(List<Long> channelIds, H5PromotionQueryDTO query) {
        if (channelIds.isEmpty()) {
            return Metrics.empty();
        }
        QueryWrapper<H5PromotionEvent> wrapper = buildEventAggregateWrapper(query)
                .select("COUNT(DISTINCT CASE WHEN event_type = 'PV' AND device_ip IS NOT NULL AND device_ip <> '' THEN CONCAT(channel_id, '#', DATE(created_at), '#', device_ip) END) AS pvCount",
                        "COUNT(DISTINCT CASE WHEN event_type = 'CLICK' AND device_ip IS NOT NULL AND device_ip <> '' THEN CONCAT(channel_id, '#', DATE(created_at), '#', device_ip) END) AS clickCount",
                        "COALESCE(SUM(CASE WHEN event_type = 'REGISTER' THEN 1 ELSE 0 END), 0) AS registerCount",
                        "COALESCE(SUM(CASE WHEN event_type = 'COMPLETE' THEN 1 ELSE 0 END), 0) AS completeCount")
                .in("channel_id", channelIds);
        List<Map<String, Object>> rows = eventMapper.selectMaps(wrapper);
        if (rows.isEmpty()) {
            return Metrics.empty();
        }
        return metricsFrom(rows.get(0));
    }

    private QueryWrapper<H5PromotionEvent> buildEventAggregateWrapper(H5PromotionQueryDTO query) {
        QueryWrapper<H5PromotionEvent> wrapper = new QueryWrapper<>();
        LocalDateTime startTime = parseStartTime(query.getStartDate());
        LocalDateTime endTime = parseEndTime(query.getEndDate());
        if (startTime != null) {
            wrapper.ge("created_at", startTime);
        }
        if (endTime != null) {
            wrapper.lt("created_at", endTime);
        }
        return wrapper;
    }

    private Channel findH5Channel(String channelCode) {
        if (!StringUtils.hasText(channelCode)) {
            throw new BizException("channelCode cannot be empty");
        }
        Channel channel = channelMapper.selectOne(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelCode, channelCode.trim())
                .eq(Channel::getChannelType, CHANNEL_TYPE_H5)
                .last("LIMIT 1"));
        if (channel == null) {
            throw new BizException("H5 channel not found: " + channelCode);
        }
        return channel;
    }

    private boolean shouldDedupeByIp(String eventType) {
        return EVENT_PV.equals(eventType) || EVENT_CLICK.equals(eventType);
    }

    private boolean hasTrackedToday(Long channelId, String eventType, String deviceIp) {
        if (channelId == null || !StringUtils.hasText(deviceIp)) {
            return false;
        }
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);
        Long count = eventMapper.selectCount(new LambdaQueryWrapper<H5PromotionEvent>()
                .eq(H5PromotionEvent::getChannelId, channelId)
                .eq(H5PromotionEvent::getEventType, eventType)
                .eq(H5PromotionEvent::getDeviceIp, deviceIp)
                .ge(H5PromotionEvent::getCreatedAt, startOfDay)
                .lt(H5PromotionEvent::getCreatedAt, startOfNextDay));
        return count != null && count > 0;
    }

    private String normalizeEventType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            throw new BizException("eventType cannot be empty");
        }
        String normalized = eventType.trim().toUpperCase(Locale.ROOT);
        if ("PAGE_VIEW".equals(normalized) || "VIEW".equals(normalized) || "EXPOSURE".equals(normalized)) {
            return EVENT_PV;
        }
        if ("PV".equals(normalized)) {
            return EVENT_PV;
        }
        if ("CLICK".equals(normalized)) {
            return EVENT_CLICK;
        }
        if ("REGISTER".equals(normalized) || "REG".equals(normalized)) {
            return EVENT_REGISTER;
        }
        if ("COMPLETE".equals(normalized) || "FINISH".equals(normalized)) {
            return EVENT_COMPLETE;
        }
        throw new BizException("eventType must be PV, CLICK, REGISTER or COMPLETE");
    }

    private List<Long> extractIds(List<Channel> channels) {
        return channels.stream()
                .map(Channel::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Metrics metricsFrom(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return Metrics.empty();
        }
        return new Metrics(
                toLong(firstPresent(row, "pvCount", "pv_count", "PVCOUNT", "PV_COUNT")),
                toLong(firstPresent(row, "clickCount", "click_count", "CLICKCOUNT", "CLICK_COUNT")),
                toLong(firstPresent(row, "registerCount", "register_count", "REGISTERCOUNT", "REGISTER_COUNT")),
                toLong(firstPresent(row, "completeCount", "complete_count", "COMPLETECOUNT", "COMPLETE_COUNT"))
        );
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
        return 0L;
    }

    private String rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return "0.00%";
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }

    private LocalDateTime parseStartTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseDateTime(value.trim(), false);
    }

    private LocalDateTime parseEndTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseDateTime(value.trim(), true);
    }

    private LocalDateTime parseDateTime(String value, boolean endExclusive) {
        String normalized = value.replace('T', ' ');
        if (normalized.length() <= 10) {
            LocalDate date = LocalDate.parse(normalized.substring(0, 10));
            return endExclusive ? date.plusDays(1).atStartOfDay() : date.atStartOfDay();
        }
        if (normalized.length() == 13) {
            normalized = normalized + ":00:00";
        } else if (normalized.length() == 16) {
            normalized = normalized + ":00";
        }
        return LocalDateTime.parse(normalized.substring(0, 19), DATE_TIME_FORMATTER);
    }

    private String firstText(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        return trimToNull(second);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record Metrics(Long pvCount, Long clickCount, Long registerCount, Long completeCount) {

        private static Metrics empty() {
            return new Metrics(0L, 0L, 0L, 0L);
        }
    }
}
