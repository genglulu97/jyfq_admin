package com.jyfq.loan.model.vo;

import com.jyfq.loan.common.result.PageResult;
import lombok.Data;

import java.io.Serializable;

/**
 * Channel report response.
 */
@Data
public class ChannelReportVO implements Serializable {

    private ChannelReportSummaryVO summary;

    private PageResult<ChannelReportRowVO> page;
}
