package com.jyfq.loan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.mapper.ApplyOrderMapper;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.model.dto.ChannelReportQueryDTO;
import com.jyfq.loan.model.dto.HourlyReportQueryDTO;
import com.jyfq.loan.model.entity.ApplyOrder;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.vo.ChannelReportRowVO;
import com.jyfq.loan.model.vo.ChannelReportSummaryVO;
import com.jyfq.loan.model.vo.ChannelReportVO;
import com.jyfq.loan.model.vo.HourlyReportRowVO;
import com.jyfq.loan.model.vo.HourlyReportVO;
import com.jyfq.loan.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Admin report aggregation service.
 */
@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private static final int SUCCESS_STATUS = 1;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ApplyOrderMapper applyOrderMapper;
    private final ChannelMapper channelMapper;

    @Override
    public ChannelReportVO channelReport(ChannelReportQueryDTO query) {
        ChannelReportVO vo = new ChannelReportVO();
        vo.setSummary(channelSummary(query));
        vo.setPage(channelRows(query));
        return vo;
    }

    @Override
    public ChannelReportSummaryVO channelSummary(ChannelReportQueryDTO query) {
        QueryWrapper<ApplyOrder> wrapper = baseOrderWrapper(query.getStartDate(), query.getEndDate());
        if (StringUtils.hasText(query.getChannelCode())) {
            wrapper.eq("channel_code", query.getChannelCode().trim());
        }
        List<Map<String, Object>> rows = applyOrderMapper.selectMaps(wrapper.select(
                "COUNT(*) AS totalOrders",
                "SUM(CASE WHEN order_status = " + SUCCESS_STATUS + " THEN 1 ELSE 0 END) AS successOrders",
                "COALESCE(SUM(settlement_price), 0) AS totalAmount"));
        Map<String, Object> row = rows.isEmpty() ? Collections.emptyMap() : rows.get(0);
        long totalOrders = toLong(row.get("totalOrders"));
        long successOrders = toLong(row.get("successOrders"));
        BigDecimal totalAmount = toBigDecimal(row.get("totalAmount"));
        ChannelReportSummaryVO summary = new ChannelReportSummaryVO();
        summary.setTotalOrders(totalOrders);
        summary.setSuccessOrders(successOrders);
        summary.setSuccessRate(percentage(successOrders, totalOrders));
        summary.setTotalAmount(totalAmount);
        summary.setAverageAmount(totalOrders == 0 ? BigDecimal.ZERO : totalAmount.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP));
        return summary;
    }

    @Override
    public PageResult<ChannelReportRowVO> channelRows(ChannelReportQueryDTO query) {
        long current = normalizeCurrent(query.getCurrent());
        long size = normalizeSize(query.getSize());
        String granularity = resolveGranularity(query);
        String statExpr = "SUMMARY".equals(granularity) ? "'SUMMARY'" : "DATE_FORMAT(created_at, '%Y-%m-%d')";
        QueryWrapper<ApplyOrder> wrapper = baseOrderWrapper(query.getStartDate(), query.getEndDate());
        if (StringUtils.hasText(query.getChannelCode())) {
            wrapper.eq("channel_code", query.getChannelCode().trim());
        }
        wrapper.select(
                        statExpr + " AS statDate",
                        "channel_id AS channelId",
                        "channel_code AS channelCode",
                        "COUNT(*) AS totalOrders",
                        "SUM(CASE WHEN order_status = " + SUCCESS_STATUS + " THEN 1 ELSE 0 END) AS successOrders",
                        "COALESCE(SUM(settlement_price), 0) AS totalAmount")
                .groupBy("SUMMARY".equals(granularity) ? "channel_id, channel_code" : "DATE_FORMAT(created_at, '%Y-%m-%d'), channel_id, channel_code")
                .orderByAsc(!"SUMMARY".equals(granularity), "statDate")
                .orderByAsc("channelCode");
        List<ChannelReportRowVO> allRows = applyOrderMapper.selectMaps(wrapper).stream()
                .map(this::toChannelReportRow)
                .toList();
        return page(current, size, allRows);
    }

    @Override
    public HourlyReportVO hourlyReport(HourlyReportQueryDTO query) {
        HourlyReportVO vo = new HourlyReportVO();
        vo.setSummary(hourlySummary(query));
        vo.setPage(hourlyRows(query));
        return vo;
    }

    @Override
    public ChannelReportSummaryVO hourlySummary(HourlyReportQueryDTO query) {
        QueryWrapper<ApplyOrder> wrapper = hourlyBaseWrapper(query);
        List<Map<String, Object>> rows = applyOrderMapper.selectMaps(wrapper.select(
                "COUNT(*) AS totalOrders",
                "SUM(CASE WHEN order_status = " + SUCCESS_STATUS + " THEN 1 ELSE 0 END) AS successOrders"));
        Map<String, Object> row = rows.isEmpty() ? Collections.emptyMap() : rows.get(0);
        long totalOrders = toLong(row.get("totalOrders"));
        long successOrders = toLong(row.get("successOrders"));
        ChannelReportSummaryVO summary = new ChannelReportSummaryVO();
        summary.setTotalOrders(totalOrders);
        summary.setSuccessOrders(successOrders);
        summary.setSuccessRate(percentage(successOrders, totalOrders));
        summary.setTotalAmount(BigDecimal.ZERO);
        summary.setAverageAmount(BigDecimal.ZERO);
        return summary;
    }

    @Override
    public PageResult<HourlyReportRowVO> hourlyRows(HourlyReportQueryDTO query) {
        long current = normalizeCurrent(query.getCurrent());
        long size = normalizeSize(query.getSize());
        QueryWrapper<ApplyOrder> wrapper = hourlyBaseWrapper(query);
        wrapper.select(
                        "DATE_FORMAT(created_at, '%Y-%m-%d %H') AS statHour",
                        "COUNT(*) AS totalOrders",
                        "SUM(CASE WHEN order_status = " + SUCCESS_STATUS + " THEN 1 ELSE 0 END) AS successOrders")
                .groupBy("DATE_FORMAT(created_at, '%Y-%m-%d %H')")
                .orderByAsc("statHour");
        List<HourlyReportRowVO> allRows = applyOrderMapper.selectMaps(wrapper).stream()
                .map(this::toHourlyReportRow)
                .toList();
        return page(current, size, allRows);
    }

    private QueryWrapper<ApplyOrder> hourlyBaseWrapper(HourlyReportQueryDTO query) {
        String startDate = StringUtils.hasText(query.getStartDate()) ? query.getStartDate() : query.getDate();
        String endDate = StringUtils.hasText(query.getEndDate()) ? query.getEndDate() : query.getDate();
        QueryWrapper<ApplyOrder> wrapper = baseOrderWrapper(startDate, endDate);
        if (query.getChannelId() != null) {
            wrapper.eq("channel_id", query.getChannelId());
        }
        if (StringUtils.hasText(query.getChannelCode())) {
            wrapper.eq("channel_code", query.getChannelCode().trim());
        }
        if (query.getInstId() != null) {
            wrapper.eq("inst_id", query.getInstId());
        }
        return wrapper;
    }

    private QueryWrapper<ApplyOrder> baseOrderWrapper(String startDate, String endDate) {
        QueryWrapper<ApplyOrder> wrapper = new QueryWrapper<>();
        LocalDateTime start = parseStart(startDate);
        LocalDateTime end = parseEnd(endDate);
        if (start != null) {
            wrapper.ge("created_at", start);
        }
        if (end != null) {
            wrapper.le("created_at", end);
        }
        return wrapper;
    }

    private String resolveGranularity(ChannelReportQueryDTO query) {
        String value = firstText(query.getGroupBy(), query.getGranularity(), query.getStatisticGranularity());
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : "DAY";
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private ChannelReportRowVO toChannelReportRow(Map<String, Object> row) {
        long totalOrders = toLong(row.get("totalOrders"));
        long successOrders = toLong(row.get("successOrders"));
        BigDecimal totalAmount = toBigDecimal(row.get("totalAmount"));
        ChannelReportRowVO vo = new ChannelReportRowVO();
        vo.setStatDate(String.valueOf(row.get("statDate")));
        vo.setChannelId(toNullableLong(row.get("channelId")));
        vo.setChannelCode(row.get("channelCode") == null ? null : String.valueOf(row.get("channelCode")));
        vo.setChannelName(resolveChannelName(vo.getChannelId(), vo.getChannelCode()));
        vo.setTotalOrders(totalOrders);
        vo.setSuccessOrders(successOrders);
        vo.setSuccessRate(percentage(successOrders, totalOrders));
        vo.setTotalAmount(totalAmount);
        vo.setAverageAmount(totalOrders == 0 ? BigDecimal.ZERO : totalAmount.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP));
        return vo;
    }

    private String resolveChannelName(Long channelId, String channelCode) {
        Channel channel = null;
        if (channelId != null) {
            channel = channelMapper.selectById(channelId);
        }
        if (channel == null && StringUtils.hasText(channelCode)) {
            channel = channelMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Channel>()
                    .eq(Channel::getChannelCode, channelCode)
                    .last("LIMIT 1"));
        }
        if (channel != null && StringUtils.hasText(channel.getChannelName())) {
            return channel.getChannelName();
        }
        return channelCode;
    }

    private HourlyReportRowVO toHourlyReportRow(Map<String, Object> row) {
        long totalOrders = toLong(row.get("totalOrders"));
        long successOrders = toLong(row.get("successOrders"));
        String statHour = String.valueOf(row.get("statHour"));
        HourlyReportRowVO vo = new HourlyReportRowVO();
        vo.setHour(statHour);
        vo.setStatHour(statHour);
        vo.setTotalOrders(totalOrders);
        vo.setSuccessOrders(successOrders);
        vo.setSuccessRate(percentage(successOrders, totalOrders));
        return vo;
    }

    private <T> PageResult<T> page(long current, long size, List<T> rows) {
        int fromIndex = (int) Math.min((current - 1) * size, rows.size());
        int toIndex = (int) Math.min(fromIndex + size, rows.size());
        return PageResult.of(current, size, rows.size(), rows.subList(fromIndex, toIndex));
    }

    private long normalizeCurrent(Long current) {
        return current == null || current < 1 ? 1L : current;
    }

    private long normalizeSize(Long size) {
        if (size == null || size < 1) {
            return 20L;
        }
        return Math.min(size, 500L);
    }

    private LocalDateTime parseStart(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 10) {
            return LocalDate.parse(trimmed).atStartOfDay();
        }
        return LocalDateTime.parse(normalizeDateTime(trimmed), DATE_TIME_FORMATTER);
    }

    private LocalDateTime parseEnd(String value) {
        if (!StringUtils.hasText(value)) {
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

    private String percentage(long part, long total) {
        if (total == 0) {
            return "0.00%";
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .toPlainString() + "%";
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

    private Long toNullableLong(Object value) {
        if (value == null) {
            return null;
        }
        return toLong(value);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }
}
