package com.jyfq.loan.model.vo;

import com.jyfq.loan.common.result.PageResult;
import lombok.Data;

import java.io.Serializable;

/**
 * Hourly report response.
 */
@Data
public class HourlyReportVO implements Serializable {

    private ChannelReportSummaryVO summary;

    private PageResult<HourlyReportRowVO> page;
}
