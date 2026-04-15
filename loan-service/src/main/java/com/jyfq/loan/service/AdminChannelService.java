package com.jyfq.loan.service;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.model.dto.ChannelQueryDTO;
import com.jyfq.loan.model.dto.ChannelSaveDTO;
import com.jyfq.loan.model.vo.ChannelListVO;

/**
 * Admin channel management service.
 */
public interface AdminChannelService {

    PageResult<ChannelListVO> pageChannels(ChannelQueryDTO query);

    Long createChannel(ChannelSaveDTO request);

    void updateChannel(Long id, ChannelSaveDTO request);

    void toggleChannel(Long id);
}
