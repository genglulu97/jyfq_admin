package com.jyfq.loan.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.common.util.AesUtil;
import com.jyfq.loan.mapper.CollisionPrecheckRecordMapper;
import com.jyfq.loan.mapper.CollisionRecordMapper;
import com.jyfq.loan.model.dto.CollisionLogQueryDTO;
import com.jyfq.loan.model.vo.CollisionLogDetailVO;
import com.jyfq.loan.model.vo.CollisionLogListVO;
import com.jyfq.loan.model.vo.CollisionLogRecordRow;
import com.jyfq.loan.service.AdminCollisionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Admin collision log query service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCollisionLogServiceImpl implements AdminCollisionLogService {

    private final CollisionRecordMapper collisionRecordMapper;
    private final CollisionPrecheckRecordMapper collisionPrecheckRecordMapper;

    @Override
    public PageResult<CollisionLogListVO> pageCollisionLogs(CollisionLogQueryDTO query) {
        CollisionLogQueryDTO safeQuery = query == null ? new CollisionLogQueryDTO() : query;
        normalizeQuery(safeQuery);

        long current = normalizePageNo(safeQuery.getCurrent());
        long size = normalizePageSize(safeQuery.getSize());
        Page<CollisionLogRecordRow> page = collisionRecordMapper.selectCollisionLogPage(new Page<>(current, size), safeQuery);
        if (page.getRecords().isEmpty()) {
            return PageResult.empty(current, size);
        }

        List<CollisionLogListVO> records = page.getRecords().stream()
                .map(this::toListVO)
                .collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public List<CollisionLogDetailVO> listCollisionLogDetails(String collisionNo) {
        if (!StringUtils.hasText(collisionNo)) {
            throw new BizException("collisionNo is required");
        }
        return collisionPrecheckRecordMapper.selectCollisionLogDetails(collisionNo.trim()).stream()
                .map(this::toDetailVO)
                .collect(Collectors.toList());
    }

    private void normalizeQuery(CollisionLogQueryDTO query) {
        if (StringUtils.hasText(query.getPhone())) {
            query.setPhoneMd5(DigestUtil.md5Hex(query.getPhone().trim()));
        }
        if (StringUtils.hasText(query.getUserName())) {
            query.setUserNameMd5(DigestUtil.md5Hex(query.getUserName().trim()));
        }
        if (StringUtils.hasText(query.getCollisionNo())) {
            query.setCollisionNo(query.getCollisionNo().trim());
        }
        if (StringUtils.hasText(query.getChannelCode())) {
            query.setChannelCode(query.getChannelCode().trim());
        }
    }

    private CollisionLogListVO toListVO(CollisionLogRecordRow row) {
        CollisionLogListVO vo = new CollisionLogListVO();
        vo.setId(row.getId());
        vo.setCollisionNo(row.getCollisionNo());
        vo.setOrderNo(row.getCollisionNo());
        vo.setRequestId(row.getRequestId());
        vo.setThirdOrderNo(row.getThirdOrderNo());
        vo.setPhone(decrypt(row.getPhoneEnc(), row.getChannelAppKey(), row.getChannelCode()));
        vo.setUserName(decrypt(row.getUserNameEnc(), row.getChannelAppKey(), row.getChannelCode()));
        vo.setChannelCode(row.getChannelCode());
        vo.setChannelName(StringUtils.hasText(row.getChannelName()) ? row.getChannelName() : row.getChannelCode());
        vo.setInstId(row.getInstId());
        vo.setInstCode(row.getInstCode());
        vo.setInstName(row.getInstName());
        vo.setProductId(row.getProductId());
        vo.setProductName(row.getProductName());
        vo.setHitRule(resolveHitRule(row));
        vo.setMatchRule(vo.getHitRule());
        vo.setPushStatus(row.getPushStatus());
        vo.setPushStatusDesc(resolvePreCheckStatusDesc(row.getPushStatus()));
        vo.setResult(Objects.equals(row.getPushStatus(), 2) ? "正常" : "异常");
        vo.setResultDesc(resolvePreCheckStatusDesc(row.getPushStatus()));
        vo.setRejectReason(resolveRejectReason(row));
        vo.setDownstreamPrice(resolveDownstreamPrice(row));
        vo.setProductCoefficientPrice(row.getProductCoefficientPrice());
        vo.setUpstreamChannelPrice(row.getUpstreamChannelPrice());
        vo.setPrice(vo.getDownstreamPrice());
        vo.setCostMs(row.getCostMs());
        vo.setPreCheckTime(row.getPushedAt());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }

    private CollisionLogDetailVO toDetailVO(CollisionLogRecordRow row) {
        CollisionLogDetailVO vo = new CollisionLogDetailVO();
        vo.setId(row.getId());
        vo.setCollisionNo(row.getCollisionNo());
        vo.setRequestId(row.getRequestId());
        vo.setThirdOrderNo(row.getThirdOrderNo());
        vo.setInstId(row.getInstId());
        vo.setInstCode(row.getInstCode());
        vo.setInstName(row.getInstName());
        vo.setProductId(row.getProductId());
        vo.setProductName(row.getProductName());
        vo.setPrecheckStatus(row.getPushStatus());
        vo.setPrecheckStatusDesc(resolvePreCheckStatusDesc(row.getPushStatus()));
        vo.setResult(Objects.equals(row.getPushStatus(), 2) ? "正常" : "异常");
        vo.setResultDesc(vo.getPrecheckStatusDesc());
        vo.setRejectReason(resolveRejectReason(row));
        vo.setDownstreamPrice(resolveDownstreamPrice(row));
        vo.setProductCoefficientPrice(row.getProductCoefficientPrice());
        vo.setUpstreamChannelPrice(row.getUpstreamChannelPrice());
        vo.setPrice(vo.getDownstreamPrice());
        vo.setCostMs(row.getCostMs());
        vo.setRequestLog(row.getRequestLog());
        vo.setResponseLog(row.getResponseLog());
        vo.setPrecheckedAt(row.getPushedAt());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }

    private String resolveHitRule(CollisionLogRecordRow row) {
        if (StringUtils.hasText(row.getProductName()) && StringUtils.hasText(row.getInstName())) {
            return row.getProductName() + " / " + row.getInstName();
        }
        if (StringUtils.hasText(row.getProductName())) {
            return row.getProductName();
        }
        if (StringUtils.hasText(row.getInstName())) {
            return row.getInstName();
        }
        return StringUtils.hasText(row.getInstCode()) ? row.getInstCode() : "-";
    }

    private String resolvePreCheckStatusDesc(Integer status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case 2 -> "撞库通过";
            case 4 -> "撞库拒绝";
            case 9 -> "撞库异常";
            default -> String.valueOf(status);
        };
    }

    private String resolveRejectReason(CollisionLogRecordRow row) {
        if (StringUtils.hasText(row.getErrorMsg())) {
            return row.getErrorMsg();
        }
        if (StringUtils.hasText(row.getCollisionRejectReason())) {
            return row.getCollisionRejectReason();
        }
        return Objects.equals(row.getPushStatus(), 2) ? null : resolvePreCheckStatusDesc(row.getPushStatus());
    }

    private BigDecimal resolveDownstreamPrice(CollisionLogRecordRow row) {
        if (row.getDownstreamPrice() != null) {
            return row.getDownstreamPrice();
        }
        return row.getSettlementPrice();
    }

    private String decrypt(String cipherText, String appKey, String channelCode) {
        if (!StringUtils.hasText(cipherText) || !StringUtils.hasText(appKey)) {
            return null;
        }
        try {
            return AesUtil.decrypt(cipherText, appKey);
        } catch (Exception ex) {
            log.warn("[ADMIN-COLLISION-LOG] decrypt failed, channelCode={}", channelCode, ex);
            return null;
        }
    }

    private long normalizePageNo(Long current) {
        return current == null || current < 1 ? 1L : current;
    }

    private long normalizePageSize(Long size) {
        if (size == null || size < 1) {
            return 20L;
        }
        return Math.min(size, 100L);
    }
}
