package com.jyfq.loan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.common.util.AuditOperatorUtil;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.model.dto.ChannelQueryDTO;
import com.jyfq.loan.model.dto.ChannelSaveDTO;
import com.jyfq.loan.model.entity.Channel;
import com.jyfq.loan.model.vo.ChannelListVO;
import com.jyfq.loan.service.AdminChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin channel management service implementation.
 */
@Service
@RequiredArgsConstructor
public class AdminChannelServiceImpl implements AdminChannelService {

    private final ChannelMapper channelMapper;

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

        List<ChannelListVO> records = page.getRecords().stream()
                .map(this::toListVO)
                .collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
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
        channel.setChannelId(request.getChannelId().trim());
        channel.setChannelName(request.getChannelName().trim());
        channel.setChannelCode(request.getChannelCode().trim());
        channel.setChannelType(request.getChannelType().trim());
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
        vo.setExtJson(channel.getExtJson());
        vo.setRemark(channel.getRemark());
        vo.setCreatedAt(channel.getCreatedAt());
        vo.setCreateBy(channel.getCreateBy());
        vo.setUpdatedAt(channel.getUpdatedAt());
        vo.setUpdateBy(channel.getUpdateBy());
        return vo;
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
}
