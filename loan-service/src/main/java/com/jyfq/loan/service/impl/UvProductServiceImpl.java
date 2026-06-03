package com.jyfq.loan.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.mapper.UvProductMapper;
import com.jyfq.loan.model.dto.UvProductDeleteDTO;
import com.jyfq.loan.model.dto.UvProductQueryDTO;
import com.jyfq.loan.model.dto.UvProductSaveDTO;
import com.jyfq.loan.model.entity.UvProduct;
import com.jyfq.loan.model.vo.H5UvProductVO;
import com.jyfq.loan.model.vo.UvProductPageVO;
import com.jyfq.loan.model.vo.UvProductVO;
import com.jyfq.loan.service.UvProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * UV product service implementation.
 */
@Service
@RequiredArgsConstructor
public class UvProductServiceImpl implements UvProductService {

    private static final String STATUS_ONLINE = "上架";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UvProductMapper uvProductMapper;

    @Override
    public UvProductPageVO pageProducts(UvProductQueryDTO query) {
        UvProductQueryDTO safeQuery = query == null ? new UvProductQueryDTO() : query;
        long current = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1L : safeQuery.getPageNum();
        long size = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1
                ? 10L
                : Math.min(safeQuery.getPageSize(), 100L);

        LambdaQueryWrapper<UvProduct> wrapper = buildListWrapper(safeQuery);
        Page<UvProduct> page = uvProductMapper.selectPage(new Page<>(current, size), wrapper);
        List<UvProductVO> list = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new UvProductPageVO(page.getTotal(), list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProduct(UvProductSaveDTO request) {
        validateSaveRequest(request, false);
        UvProduct product = new UvProduct();
        fillProduct(product, request);
        uvProductMapper.insert(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(UvProductSaveDTO request) {
        validateSaveRequest(request, true);
        UvProduct existing = uvProductMapper.selectById(request.getId());
        if (existing == null) {
            throw new BizException("UV product not found: " + request.getId());
        }
        fillProduct(existing, request);
        uvProductMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProducts(UvProductDeleteDTO request) {
        if (request == null || CollectionUtils.isEmpty(request.getIds())) {
            throw new BizException("ids is required");
        }
        List<Long> ids = request.getIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            throw new BizException("ids is required");
        }
        uvProductMapper.deleteBatchIds(ids);
    }

    @Override
    public List<H5UvProductVO> listH5Products() {
        return uvProductMapper.selectList(new LambdaQueryWrapper<UvProduct>()
                        .eq(UvProduct::getStatus, STATUS_ONLINE)
                        .orderByDesc(UvProduct::getWeight)
                        .orderByDesc(UvProduct::getId))
                .stream()
                .map(this::toH5VO)
                .collect(Collectors.toList());
    }

    private LambdaQueryWrapper<UvProduct> buildListWrapper(UvProductQueryDTO query) {
        LambdaQueryWrapper<UvProduct> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(UvProduct::getName, query.getName().trim());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(UvProduct::getStatus, query.getStatus().trim());
        }
        if (StringUtils.hasText(query.getIsJoint())) {
            wrapper.eq(UvProduct::getIsJoint, query.getIsJoint().trim());
        }
        if (StringUtils.hasText(query.getPosition())) {
            wrapper.eq(UvProduct::getPosition, query.getPosition().trim());
        }
        LocalDateTime startTime = parseDateTime(query.getCreateTimeStart(), false);
        LocalDateTime endTime = parseDateTime(query.getCreateTimeEnd(), true);
        if (startTime != null) {
            wrapper.ge(UvProduct::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(UvProduct::getCreatedAt, endTime);
        }
        wrapper.orderByDesc(UvProduct::getCreatedAt).orderByDesc(UvProduct::getId);
        return wrapper;
    }

    private void validateSaveRequest(UvProductSaveDTO request, boolean update) {
        if (request == null) {
            throw new BizException("request is required");
        }
        if (update && request.getId() == null) {
            throw new BizException("id is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("name is required");
        }
        if (request.getMinAmount() != null && request.getMaxAmount() != null
                && request.getMinAmount() > request.getMaxAmount()) {
            throw new BizException("amount range is invalid");
        }
        if (request.getMinAge() != null && request.getMaxAge() != null
                && request.getMinAge() > request.getMaxAge()) {
            throw new BizException("age range is invalid");
        }
    }

    private void fillProduct(UvProduct product, UvProductSaveDTO request) {
        product.setName(trimToNull(request.getName()));
        product.setLogo(trimToNull(request.getLogo()));
        product.setStatus(defaultText(request.getStatus(), STATUS_ONLINE));
        product.setPosition(trimToNull(request.getPosition()));
        product.setLoanType(trimToNull(request.getLoanType()));
        product.setMinAmount(request.getMinAmount());
        product.setMaxAmount(request.getMaxAmount());
        product.setRate(trimToNull(request.getRate()));
        product.setTerm(trimToNull(request.getTerm()));
        product.setWeight(defaultInt(request.getWeight(), 0));
        product.setPrice(request.getPrice());
        product.setUvThreshold(request.getUvThreshold());
        product.setBadge(trimToNull(request.getBadge()));
        product.setIsJoint(defaultText(request.getIsJoint(), "否"));
        product.setApplyUrl(trimToNull(request.getApplyUrl()));
        product.setJointChannel(trimToNull(request.getJointChannel()));
        product.setJointKey(trimToNull(request.getJointKey()));
        product.setJointCheckUrl(trimToNull(request.getJointCheckUrl()));
        product.setJointLoginUrl(trimToNull(request.getJointLoginUrl()));
        product.setJointRegAgreement(trimToNull(request.getJointRegAgreement()));
        product.setAutoTimeStart(request.getAutoTimeStart());
        product.setAutoTimeEnd(request.getAutoTimeEnd());
        product.setAutoOfflineTime(request.getAutoOfflineTime() == null
                ? request.getAutoTimeEnd()
                : request.getAutoOfflineTime());
        product.setAssocInst(trimToNull(request.getAssocInst()));
        product.setSpecChannels(trimToNull(request.getSpecChannels()));
        product.setMinAge(request.getMinAge());
        product.setMaxAge(request.getMaxAge());
        product.setBlockProvinces(trimToNull(request.getBlockProvinces()));
        product.setBlockCities(trimToNull(request.getBlockCities()));
        product.setTargetRegions(trimToNull(request.getTargetRegions()));
        product.setApplyCount(defaultInt(request.getApplyCount(), 0));
        product.setZhima(toJsonArray(request.getZhima()));
        product.setHouse(toJsonArray(request.getHouse()));
        product.setCar(toJsonArray(request.getCar()));
        product.setGongjijin(toJsonArray(request.getGongjijin()));
        product.setJob(toJsonArray(request.getJob()));
    }

    private UvProductVO toVO(UvProduct product) {
        UvProductVO vo = new UvProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setLogo(product.getLogo());
        vo.setStatus(product.getStatus());
        vo.setPosition(product.getPosition());
        vo.setLoanType(product.getLoanType());
        vo.setMinAmount(product.getMinAmount());
        vo.setMaxAmount(product.getMaxAmount());
        vo.setRate(product.getRate());
        vo.setTerm(product.getTerm());
        vo.setWeight(product.getWeight());
        vo.setPrice(product.getPrice());
        vo.setUvThreshold(product.getUvThreshold());
        vo.setBadge(product.getBadge());
        vo.setIsJoint(product.getIsJoint());
        vo.setApplyUrl(product.getApplyUrl());
        vo.setJointChannel(product.getJointChannel());
        vo.setJointKey(product.getJointKey());
        vo.setJointCheckUrl(product.getJointCheckUrl());
        vo.setJointLoginUrl(product.getJointLoginUrl());
        vo.setJointRegAgreement(product.getJointRegAgreement());
        vo.setAutoTimeStart(product.getAutoTimeStart());
        vo.setAutoTimeEnd(product.getAutoTimeEnd());
        vo.setAutoOfflineTime(product.getAutoOfflineTime() == null ? product.getAutoTimeEnd() : product.getAutoOfflineTime());
        vo.setAssocInst(product.getAssocInst());
        vo.setSpecChannels(product.getSpecChannels());
        vo.setMinAge(product.getMinAge());
        vo.setMaxAge(product.getMaxAge());
        vo.setBlockProvinces(product.getBlockProvinces());
        vo.setBlockCities(product.getBlockCities());
        vo.setTargetRegions(product.getTargetRegions());
        vo.setApplyCount(defaultInt(product.getApplyCount(), 0));
        vo.setCreatedAt(product.getCreatedAt());
        vo.setZhima(parseJsonArray(product.getZhima()));
        vo.setHouse(parseJsonArray(product.getHouse()));
        vo.setCar(parseJsonArray(product.getCar()));
        vo.setGongjijin(parseJsonArray(product.getGongjijin()));
        vo.setJob(parseJsonArray(product.getJob()));
        return vo;
    }

    private H5UvProductVO toH5VO(UvProduct product) {
        H5UvProductVO vo = new H5UvProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setLogo(product.getLogo());
        vo.setLoanType(product.getLoanType());
        vo.setMinAmount(product.getMinAmount());
        vo.setMaxAmount(product.getMaxAmount());
        vo.setRate(product.getRate());
        vo.setTerm(product.getTerm());
        vo.setBadge(product.getBadge());
        vo.setApplyUrl(product.getApplyUrl());
        vo.setWeight(product.getWeight());
        return vo;
    }

    private LocalDateTime parseDateTime(String value, boolean endTime) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() == 10) {
            normalized = normalized + (endTime ? " 23:59:59" : " 00:00:00");
        }
        try {
            return LocalDateTime.parse(normalized.substring(0, 19), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException | IndexOutOfBoundsException ex) {
            throw new BizException("invalid datetime: " + value);
        }
    }

    private String toJsonArray(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        List<String> normalized = values.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return normalized.isEmpty() ? null : JSON.toJSONString(normalized);
    }

    private List<String> parseJsonArray(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(value, String.class);
        } catch (Exception ex) {
            return List.of(value);
        }
    }

    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String defaultText(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
