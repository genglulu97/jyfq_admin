package com.jyfq.loan.service.upstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.ResultCode;
import com.jyfq.loan.mapper.ChannelMapper;
import com.jyfq.loan.model.entity.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelAccessGuard {

    private final ChannelMapper channelMapper;

    public void validate(String channelCode, String clientIp) {
        if (!StringUtils.hasText(channelCode)) {
            throw new BizException(ResultCode.CHANNEL_NOT_FOUND, channelCode);
        }
        Channel channel = channelMapper.selectOne(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelCode, channelCode.trim())
                .last("LIMIT 1"));
        validate(channel, clientIp);
    }

    public void validate(Channel channel, String clientIp) {
        if (channel == null) {
            throw new BizException(ResultCode.CHANNEL_NOT_FOUND, null);
        }
        String whitelist = channel.getIpWhitelist();
        if (!StringUtils.hasText(whitelist)) {
            return;
        }
        if (!StringUtils.hasText(clientIp) || !matchesWhitelist(clientIp, whitelist)) {
            log.warn("[CHANNEL-IP] blocked, channelCode={}, clientIp={}, whitelist={}",
                    channel.getChannelCode(), clientIp, whitelist);
            throw new BizException(ResultCode.IP_BLOCKED, clientIp);
        }
    }

    private boolean matchesWhitelist(String clientIp, String whitelist) {
        String normalizedClientIp = normalizeIp(clientIp);
        return Arrays.stream(whitelist.split("[,;\\s]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .anyMatch(allowedIp -> "*".equals(allowedIp)
                        || allowedIp.equals(clientIp.trim())
                        || normalizeIp(allowedIp).equals(normalizedClientIp));
    }

    private String normalizeIp(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            trimmed = trimmed.substring(1, trimmed.indexOf(']'));
        }
        try {
            return InetAddress.getByName(trimmed).getHostAddress();
        } catch (Exception ex) {
            return trimmed;
        }
    }
}
