package com.jyfq.loan.service;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.model.dto.ChannelReportQueryDTO;
import com.jyfq.loan.model.dto.HourlyReportQueryDTO;
import com.jyfq.loan.model.vo.ChannelReportRowVO;
import com.jyfq.loan.model.vo.ChannelReportSummaryVO;
import com.jyfq.loan.model.vo.ChannelReportVO;
import com.jyfq.loan.model.vo.HourlyReportRowVO;
import com.jyfq.loan.model.vo.HourlyReportVO;

public interface AdminReportService {

    ChannelReportVO channelReport(ChannelReportQueryDTO query);

    ChannelReportSummaryVO channelSummary(ChannelReportQueryDTO query);

    PageResult<ChannelReportRowVO> channelRows(ChannelReportQueryDTO query);

    HourlyReportVO hourlyReport(HourlyReportQueryDTO query);

    ChannelReportSummaryVO hourlySummary(HourlyReportQueryDTO query);

    PageResult<HourlyReportRowVO> hourlyRows(HourlyReportQueryDTO query);
}
