package com.jyfq.loan.service;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.model.dto.H5PromotionQueryDTO;
import com.jyfq.loan.model.dto.H5TrackDTO;
import com.jyfq.loan.model.vo.H5PromotionListVO;
import com.jyfq.loan.model.vo.H5PromotionSummaryVO;

public interface H5PromotionService {

    PageResult<H5PromotionListVO> pagePromotions(H5PromotionQueryDTO query);

    H5PromotionSummaryVO summary(H5PromotionQueryDTO query);

    void track(H5TrackDTO request, String clientIp, String userAgent, String referer);

    void trackComplete(String channelCode, String clientIp, String userAgent, String referer);
}
