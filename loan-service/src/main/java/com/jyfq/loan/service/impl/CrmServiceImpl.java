package com.jyfq.loan.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.common.exception.BizException;
import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.mapper.CrmAssignmentRecordMapper;
import com.jyfq.loan.mapper.CrmCustomerMapper;
import com.jyfq.loan.mapper.CrmEmployeeProfileMapper;
import com.jyfq.loan.mapper.CrmFollowRecordMapper;
import com.jyfq.loan.mapper.CrmPublicPoolRecordMapper;
import com.jyfq.loan.mapper.CrmTeamMapper;
import com.jyfq.loan.mapper.SysAdminMapper;
import com.jyfq.loan.model.dto.CrmCurrentUserDTO;
import com.jyfq.loan.model.dto.CrmCustomerAssignDTO;
import com.jyfq.loan.model.dto.CrmCustomerBatchImportDTO;
import com.jyfq.loan.model.dto.CrmCustomerQueryDTO;
import com.jyfq.loan.model.dto.CrmCustomerSaveDTO;
import com.jyfq.loan.model.dto.CrmEmployeeProfileQueryDTO;
import com.jyfq.loan.model.dto.CrmEmployeeProfileSaveDTO;
import com.jyfq.loan.model.dto.CrmFollowRecordQueryDTO;
import com.jyfq.loan.model.dto.CrmFollowRecordSaveDTO;
import com.jyfq.loan.model.dto.CrmPublicPoolQueryDTO;
import com.jyfq.loan.model.entity.CrmAssignmentRecord;
import com.jyfq.loan.model.entity.CrmCustomer;
import com.jyfq.loan.model.entity.CrmEmployeeProfile;
import com.jyfq.loan.model.entity.CrmFollowRecord;
import com.jyfq.loan.model.entity.CrmPublicPoolRecord;
import com.jyfq.loan.model.entity.CrmTeam;
import com.jyfq.loan.model.entity.SysAdmin;
import com.jyfq.loan.model.vo.CrmCustomerDetailVO;
import com.jyfq.loan.model.vo.CrmCustomerVO;
import com.jyfq.loan.model.vo.CrmDashboardVO;
import com.jyfq.loan.model.vo.CrmEmployeeProfileVO;
import com.jyfq.loan.model.vo.CrmFollowRecordVO;
import com.jyfq.loan.model.vo.CrmImportResultVO;
import com.jyfq.loan.model.vo.CrmTeamSummaryVO;
import com.jyfq.loan.service.CrmService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrmServiceImpl implements CrmService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;

    private final CrmCustomerMapper crmCustomerMapper;
    private final CrmFollowRecordMapper crmFollowRecordMapper;
    private final CrmAssignmentRecordMapper crmAssignmentRecordMapper;
    private final CrmPublicPoolRecordMapper crmPublicPoolRecordMapper;
    private final CrmEmployeeProfileMapper crmEmployeeProfileMapper;
    private final CrmTeamMapper crmTeamMapper;
    private final SysAdminMapper sysAdminMapper;

    @Override
    public PageResult<CrmCustomerVO> pageCustomers(CrmCustomerQueryDTO query, CrmCurrentUserDTO currentUser) {
        CrmCustomerQueryDTO safeQuery = query == null ? new CrmCustomerQueryDTO() : query;
        Page<CrmCustomer> page = crmCustomerMapper.selectPage(new Page<>(pageCurrent(safeQuery.getCurrent()), pageSize(safeQuery.getSize())),
                customerWrapper(safeQuery, currentUser));
        List<CrmCustomerVO> records = page.getRecords().stream()
                .map(customer -> toCustomerVO(customer, canSeeSensitive(customer, currentUser)))
                .collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public PageResult<CrmCustomerVO> pageMyCustomers(CrmCustomerQueryDTO query, CrmCurrentUserDTO currentUser) {
        CrmCustomerQueryDTO safeQuery = query == null ? new CrmCustomerQueryDTO() : query;
        safeQuery.setOwnerAdminId(currentUser.getAdminId());
        safeQuery.setInPublicPool(0);
        return pageCustomers(safeQuery, currentUser);
    }

    @Override
    public PageResult<CrmCustomerVO> pagePublicPool(CrmPublicPoolQueryDTO query, CrmCurrentUserDTO currentUser) {
        CrmCustomerQueryDTO customerQuery = new CrmCustomerQueryDTO();
        customerQuery.setCurrent(query == null ? null : query.getCurrent());
        customerQuery.setSize(query == null ? null : query.getSize());
        customerQuery.setCustomerName(query == null ? null : query.getCustomerName());
        customerQuery.setMobile(query == null ? null : query.getMobile());
        customerQuery.setCity(query == null ? null : query.getCity());
        customerQuery.setLoanIntention(query == null ? null : query.getLoanIntention());
        customerQuery.setQualityStar(query == null ? null : query.getQualityStar());
        customerQuery.setInPublicPool(1);
        LambdaQueryWrapper<CrmCustomer> wrapper = customerWrapper(customerQuery, currentUser);
        if (query != null && StringUtils.hasText(query.getPublicPoolReason())) {
            wrapper.like(CrmCustomer::getPublicPoolReason, query.getPublicPoolReason().trim());
        }
        Page<CrmCustomer> page = crmCustomerMapper.selectPage(new Page<>(pageCurrent(customerQuery.getCurrent()), pageSize(customerQuery.getSize())), wrapper);
        List<CrmCustomerVO> records = page.getRecords().stream()
                .map(customer -> toCustomerVO(customer, canSeeSensitive(customer, currentUser)))
                .collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public CrmCustomerDetailVO customerDetail(Long id, CrmCurrentUserDTO currentUser) {
        CrmCustomer customer = requireCustomer(id);
        assertCustomerVisible(customer, currentUser);
        CrmCustomerDetailVO vo = new CrmCustomerDetailVO();
        BeanUtils.copyProperties(toCustomerVO(customer, canSeeSensitive(customer, currentUser)), vo);
        BeanUtils.copyProperties(customer, vo);
        if (!canSeeSensitive(customer, currentUser)) {
            vo.setMobile(null);
            vo.setIdCard(null);
        }
        List<CrmFollowRecordVO> records = crmFollowRecordMapper.selectList(new LambdaQueryWrapper<CrmFollowRecord>()
                        .eq(CrmFollowRecord::getCustomerId, id)
                        .orderByDesc(CrmFollowRecord::getFollowTime)
                        .orderByDesc(CrmFollowRecord::getId))
                .stream()
                .map(record -> toFollowRecordVO(record, canSeeSensitive(customer, currentUser)))
                .collect(Collectors.toList());
        vo.setFollowRecords(records);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCustomer(CrmCustomerSaveDTO request, CrmCurrentUserDTO currentUser) {
        if (currentUser.isOperator() && request.getOwnerAdminId() != null && !Objects.equals(request.getOwnerAdminId(), currentUser.getAdminId())) {
            throw new BizException("operator cannot create customer for another owner");
        }
        CrmCustomer customer = new CrmCustomer();
        fillCustomer(customer, request);
        if (customer.getOwnerAdminId() == null && currentUser.isOperator()) {
            customer.setOwnerAdminId(currentUser.getAdminId());
            customer.setOwnerName(currentUser.getRealName());
            customer.setTeamId(currentUser.getTeamId());
        }
        if (customer.getOwnerAdminId() != null) {
            fillOwner(customer, customer.getOwnerAdminId());
            customer.setIsAllocated(1);
            customer.setInPublicPool(0);
        } else {
            customer.setIsAllocated(0);
            customer.setInPublicPool(1);
            customer.setPublicPoolReason("NEW_UNASSIGNED");
        }
        customer.setIsDuplicate(isDuplicateMobile(null, customer.getMobile()) ? 1 : 0);
        applyCustomerDefaults(customer);
        crmCustomerMapper.insert(customer);
        if (Integer.valueOf(1).equals(customer.getInPublicPool())) {
            writePublicPoolRecord(customer, null, currentUser, "NEW_UNASSIGNED", "ENTER", null);
        }
        return customer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomer(Long id, CrmCustomerSaveDTO request, CrmCurrentUserDTO currentUser) {
        CrmCustomer existing = requireCustomer(id);
        assertCustomerWritable(existing, currentUser);
        CrmCustomer customer = new CrmCustomer();
        customer.setId(id);
        fillCustomer(customer, request);
        if (request.getOwnerAdminId() != null && currentUser.isOperator() && !Objects.equals(request.getOwnerAdminId(), currentUser.getAdminId())) {
            throw new BizException("operator cannot transfer customer");
        }
        if (request.getOwnerAdminId() != null) {
            fillOwner(customer, request.getOwnerAdminId());
            customer.setIsAllocated(1);
            customer.setInPublicPool(0);
            customer.setPublicPoolReason(null);
        }
        customer.setIsDuplicate(isDuplicateMobile(id, customer.getMobile()) ? 1 : 0);
        crmCustomerMapper.updateById(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomer(Long id, CrmCurrentUserDTO currentUser) {
        if (!currentUser.isAdmin()) {
            throw new BizException("only admin can delete customer");
        }
        crmCustomerMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignCustomers(CrmCustomerAssignDTO request, CrmCurrentUserDTO currentUser) {
        CrmEmployeeProfile assignee = requireActiveProfile(request.getToAdminId());
        if (currentUser.isSupervisor() && !Objects.equals(currentUser.getTeamId(), assignee.getTeamId())) {
            throw new BizException("supervisor can only assign within own team");
        }
        for (Long customerId : request.getCustomerIds()) {
            CrmCustomer customer = requireCustomer(customerId);
            assertCustomerWritable(customer, currentUser);
            CrmCustomer update = new CrmCustomer();
            crmCustomerMapper.update(update, new LambdaUpdateWrapper<CrmCustomer>()
                    .eq(CrmCustomer::getId, customerId)
                    .set(CrmCustomer::getOwnerAdminId, assignee.getAdminId())
                    .set(CrmCustomer::getOwnerName, assignee.getEmployeeName())
                    .set(CrmCustomer::getTeamId, assignee.getTeamId())
                    .set(CrmCustomer::getIsAllocated, 1)
                    .set(CrmCustomer::getInPublicPool, 0)
                    .set(CrmCustomer::getPublicPoolReason, null));
            writeAssignmentRecord(customer, assignee, currentUser, defaultText(request.getAssignMode(), "MANUAL"), request.getRemark());
            if (Integer.valueOf(1).equals(customer.getInPublicPool())) {
                writePublicPoolRecord(customer, assignee, currentUser, "ASSIGNED_FROM_PUBLIC_POOL", "LEAVE", request.getRemark());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reclaimCustomers(List<Long> customerIds, String reason, CrmCurrentUserDTO currentUser) {
        if (customerIds == null || customerIds.isEmpty()) {
            throw new BizException("customerIds is required");
        }
        String finalReason = defaultText(reason, "MANUAL_RECLAIM");
        for (Long customerId : customerIds) {
            CrmCustomer customer = requireCustomer(customerId);
            assertCustomerWritable(customer, currentUser);
            if (customer.getQualityStar() != null && customer.getQualityStar() >= 4 && currentUser.isOperator()) {
                throw new BizException("high quality customer requires supervisor confirmation");
            }
            CrmCustomer update = new CrmCustomer();
            crmCustomerMapper.update(update, new LambdaUpdateWrapper<CrmCustomer>()
                    .eq(CrmCustomer::getId, customerId)
                    .set(CrmCustomer::getOwnerAdminId, null)
                    .set(CrmCustomer::getOwnerName, null)
                    .set(CrmCustomer::getIsAllocated, 0)
                    .set(CrmCustomer::getInPublicPool, 1)
                    .set(CrmCustomer::getPublicPoolReason, finalReason));
            writePublicPoolRecord(customer, null, currentUser, finalReason, "ENTER", null);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimCustomer(Long customerId, CrmCurrentUserDTO currentUser) {
        CrmCustomer customer = requireCustomer(customerId);
        if (!Integer.valueOf(1).equals(customer.getInPublicPool())) {
            throw new BizException("customer is not in public pool");
        }
        if (customer.getQualityStar() != null && customer.getQualityStar() >= 4 && currentUser.isOperator()) {
            throw new BizException("high quality customer must be assigned by supervisor");
        }
        CrmEmployeeProfile profile = requireActiveProfile(currentUser.getAdminId());
        int dailyLimit = profile.getDailyClaimLimit() == null ? 50 : profile.getDailyClaimLimit();
        if (dailyClaimCount(currentUser.getAdminId()) >= dailyLimit) {
            throw new BizException("daily claim limit exceeded");
        }
        CrmCustomerAssignDTO assignDTO = new CrmCustomerAssignDTO();
        assignDTO.setCustomerIds(List.of(customerId));
        assignDTO.setToAdminId(currentUser.getAdminId());
        assignDTO.setAssignMode("CLAIM");
        assignDTO.setRemark("claim from public pool");
        assignCustomers(assignDTO, currentUser);
        writePublicPoolRecord(customer, profile, currentUser, "CLAIM", "CLAIM", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFollowRecord(CrmFollowRecordSaveDTO request, CrmCurrentUserDTO currentUser) {
        CrmCustomer customer = requireCustomer(request.getCustomerId());
        assertCustomerWritable(customer, currentUser);
        if (isPositiveIntention(request.getLoanIntention()) && request.getNextFollowTime() == null) {
            throw new BizException("intention customer must have nextFollowTime");
        }
        CrmFollowRecord record = new CrmFollowRecord();
        record.setCustomerId(customer.getId());
        record.setCustomerName(customer.getCustomerName());
        record.setMobile(customer.getMobile());
        record.setFollowerAdminId(currentUser.getAdminId());
        record.setFollowerName(currentUser.getRealName());
        record.setTeamId(currentUser.getTeamId() == null ? customer.getTeamId() : currentUser.getTeamId());
        record.setFollowMethod(defaultText(request.getFollowMethod(), "PHONE"));
        record.setFollowResult(request.getFollowResult());
        record.setLoanIntention(request.getLoanIntention());
        record.setQualityStar(request.getQualityStar());
        record.setCustomerStatus(request.getCustomerStatus());
        record.setRemark(request.getRemark());
        record.setFollowTime(request.getFollowTime() == null ? LocalDateTime.now() : request.getFollowTime());
        record.setNextFollowTime(request.getNextFollowTime());
        crmFollowRecordMapper.insert(record);

        CrmCustomer update = new CrmCustomer();
        update.setId(customer.getId());
        update.setFollowCount(customer.getFollowCount() == null ? 1 : customer.getFollowCount() + 1);
        update.setLastFollowTime(record.getFollowTime());
        update.setNextFollowTime(request.getNextFollowTime());
        update.setLastFollowRemark(request.getRemark());
        update.setIsCalled("PHONE".equalsIgnoreCase(record.getFollowMethod()) ? 1 : customer.getIsCalled());
        update.setLoanIntention(firstText(request.getLoanIntention(), customer.getLoanIntention()));
        update.setQualityStar(request.getQualityStar() == null ? customer.getQualityStar() : request.getQualityStar());
        update.setCustomerStatus(firstText(request.getCustomerStatus(), customer.getCustomerStatus()));
        update.setIsValid(request.getIsValid());
        update.setWechatAdded(request.getWechatAdded());
        update.setNeedRecall(request.getNeedRecall());
        update.setIsDeal(request.getIsDeal());
        update.setIsRejected(request.getIsRejected());
        Integer star = update.getQualityStar() == null ? customer.getQualityStar() : update.getQualityStar();
        update.setIsKeyCustomer(star != null && star >= 4 ? 1 : customer.getIsKeyCustomer());
        crmCustomerMapper.updateById(update);
        return record.getId();
    }

    @Override
    public PageResult<CrmFollowRecordVO> pageFollowRecords(CrmFollowRecordQueryDTO query, CrmCurrentUserDTO currentUser) {
        CrmFollowRecordQueryDTO safeQuery = query == null ? new CrmFollowRecordQueryDTO() : query;
        Page<CrmFollowRecord> page = crmFollowRecordMapper.selectPage(new Page<>(pageCurrent(safeQuery.getCurrent()), pageSize(safeQuery.getSize())),
                followWrapper(safeQuery, currentUser));
        List<CrmFollowRecordVO> records = page.getRecords().stream()
                .map(record -> toFollowRecordVO(record, currentUser.isAdmin() || Objects.equals(record.getFollowerAdminId(), currentUser.getAdminId())))
                .collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public CrmDashboardVO dashboard(CrmCurrentUserDTO currentUser) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        CrmDashboardVO vo = new CrmDashboardVO();
        vo.setTodayNewCustomerCount(countCustomers(new LambdaQueryWrapper<CrmCustomer>().ge(CrmCustomer::getCreatedAt, start).lt(CrmCustomer::getCreatedAt, end), currentUser));
        vo.setTodayFollowedCustomerCount(countFollows(new LambdaQueryWrapper<CrmFollowRecord>().ge(CrmFollowRecord::getFollowTime, start).lt(CrmFollowRecord::getFollowTime, end), currentUser));
        vo.setTodayPendingFollowCount(countCustomers(new LambdaQueryWrapper<CrmCustomer>().ge(CrmCustomer::getNextFollowTime, start).lt(CrmCustomer::getNextFollowTime, end), currentUser));
        vo.setTodayCallCount(countFollows(new LambdaQueryWrapper<CrmFollowRecord>().eq(CrmFollowRecord::getFollowMethod, "PHONE").ge(CrmFollowRecord::getFollowTime, start).lt(CrmFollowRecord::getFollowTime, end), currentUser));
        vo.setIntentionCustomerCount(countCustomers(new LambdaQueryWrapper<CrmCustomer>().in(CrmCustomer::getLoanIntention, "MEDIUM", "STRONG", "APPLIED", "DEAL"), currentUser));
        vo.setHighQualityCustomerCount(countCustomers(new LambdaQueryWrapper<CrmCustomer>().ge(CrmCustomer::getQualityStar, 4), currentUser));
        vo.setUnfollowedCustomerCount(countCustomers(new LambdaQueryWrapper<CrmCustomer>().eq(CrmCustomer::getFollowCount, 0), currentUser));
        vo.setPublicPoolCustomerCount(countCustomers(new LambdaQueryWrapper<CrmCustomer>().eq(CrmCustomer::getInPublicPool, 1), currentUser));
        vo.setMyCustomerCount(crmCustomerMapper.selectCount(new LambdaQueryWrapper<CrmCustomer>().eq(CrmCustomer::getOwnerAdminId, currentUser.getAdminId()).eq(CrmCustomer::getInPublicPool, 0)));
        vo.setTeamCustomerCount(countCustomers(new LambdaQueryWrapper<CrmCustomer>(), currentUser));
        vo.setTodayPendingFollowCustomers(limitedCustomers(new LambdaQueryWrapper<CrmCustomer>().ge(CrmCustomer::getNextFollowTime, start).lt(CrmCustomer::getNextFollowTime, end).orderByAsc(CrmCustomer::getNextFollowTime), currentUser, 10));
        vo.setRecentFollowRecords(limitedFollows(new LambdaQueryWrapper<CrmFollowRecord>().orderByDesc(CrmFollowRecord::getFollowTime), currentUser, 10));
        vo.setCustomerStatusDistribution(groupCustomer("customer_status", currentUser));
        vo.setQualityStarDistribution(groupCustomer("quality_star", currentUser));
        vo.setSourceDistribution(groupCustomer("customer_source", currentUser));
        vo.setEmployeeFollowRanking(groupFollow("follower_name", currentUser));
        return vo;
    }

    @Override
    public CrmTeamSummaryVO teamSummary(Long teamId, CrmCurrentUserDTO currentUser) {
        Long finalTeamId = teamId == null ? currentUser.getTeamId() : teamId;
        if (finalTeamId == null) {
            throw new BizException("teamId is required");
        }
        if (currentUser.isSupervisor() && !Objects.equals(finalTeamId, currentUser.getTeamId())) {
            throw new BizException("supervisor can only view own team");
        }
        CrmCurrentUserDTO teamUser = cloneForTeam(currentUser, finalTeamId);
        CrmTeamSummaryVO vo = new CrmTeamSummaryVO();
        vo.setTeamId(finalTeamId);
        vo.setCustomerCount(countCustomers(new LambdaQueryWrapper<CrmCustomer>().eq(CrmCustomer::getTeamId, finalTeamId), currentUser.isAdmin() ? null : teamUser));
        CrmEmployeeProfileQueryDTO query = new CrmEmployeeProfileQueryDTO();
        query.setTeamId(finalTeamId);
        query.setSize(200L);
        vo.setEmployees(pageEmployeeProfiles(query, currentUser).getRecords());
        vo.setStatusDistribution(groupCustomer("customer_status", teamUser));
        vo.setIntentionDistribution(groupCustomer("loan_intention", teamUser));
        vo.setQualityStarDistribution(groupCustomer("quality_star", teamUser));
        return vo;
    }

    @Override
    public PageResult<CrmEmployeeProfileVO> pageEmployeeProfiles(CrmEmployeeProfileQueryDTO query, CrmCurrentUserDTO currentUser) {
        CrmEmployeeProfileQueryDTO safeQuery = query == null ? new CrmEmployeeProfileQueryDTO() : query;
        LambdaQueryWrapper<CrmEmployeeProfile> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(safeQuery.getEmployeeName())) {
            wrapper.like(CrmEmployeeProfile::getEmployeeName, safeQuery.getEmployeeName().trim());
        }
        if (StringUtils.hasText(safeQuery.getCrmRole())) {
            wrapper.eq(CrmEmployeeProfile::getCrmRole, safeQuery.getCrmRole().trim());
        }
        if (safeQuery.getStatus() != null) {
            wrapper.eq(CrmEmployeeProfile::getStatus, safeQuery.getStatus());
        }
        if (safeQuery.getTeamId() != null) {
            wrapper.eq(CrmEmployeeProfile::getTeamId, safeQuery.getTeamId());
        } else if (currentUser.isSupervisor()) {
            wrapper.eq(CrmEmployeeProfile::getTeamId, currentUser.getTeamId());
        } else if (currentUser.isOperator()) {
            wrapper.eq(CrmEmployeeProfile::getAdminId, currentUser.getAdminId());
        }
        wrapper.orderByDesc(CrmEmployeeProfile::getCreatedAt).orderByDesc(CrmEmployeeProfile::getId);
        Page<CrmEmployeeProfile> page = crmEmployeeProfileMapper.selectPage(new Page<>(pageCurrent(safeQuery.getCurrent()), pageSize(safeQuery.getSize())), wrapper);
        List<CrmEmployeeProfileVO> records = page.getRecords().stream()
                .map(this::toEmployeeProfileVO)
                .collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveEmployeeProfile(CrmEmployeeProfileSaveDTO request, CrmCurrentUserDTO currentUser) {
        if (!currentUser.isAdmin()) {
            throw new BizException("only admin can save employee profile");
        }
        SysAdmin admin = sysAdminMapper.selectById(request.getAdminId());
        if (admin == null) {
            throw new BizException("admin user not found");
        }
        CrmEmployeeProfile existing = findProfile(request.getAdminId());
        CrmEmployeeProfile profile = existing == null ? new CrmEmployeeProfile() : existing;
        profile.setAdminId(request.getAdminId());
        profile.setEmployeeName(defaultText(request.getEmployeeName(), StringUtils.hasText(admin.getRealName()) ? admin.getRealName() : admin.getUsername()));
        profile.setPhone(request.getPhone());
        profile.setTeamId(request.getTeamId());
        profile.setCrmRole(defaultText(request.getCrmRole(), admin.getRole()));
        profile.setDailyClaimLimit(request.getDailyClaimLimit() == null ? 50 : request.getDailyClaimLimit());
        profile.setAssignWeight(request.getAssignWeight() == null ? 1 : request.getAssignWeight());
        profile.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        profile.setDataScope(defaultText(request.getDataScope(), "SELF"));
        profile.setRemark(request.getRemark());
        if (existing == null) {
            crmEmployeeProfileMapper.insert(profile);
        } else {
            crmEmployeeProfileMapper.updateById(profile);
        }
        return profile.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmImportResultVO batchImportCustomers(CrmCustomerBatchImportDTO request, CrmCurrentUserDTO currentUser) {
        CrmImportResultVO result = new CrmImportResultVO();
        if (request == null || request.getCustomers() == null || request.getCustomers().isEmpty()) {
            result.getErrors().add("customers is empty");
            return result;
        }
        for (int i = 0; i < request.getCustomers().size(); i++) {
            CrmCustomerSaveDTO item = request.getCustomers().get(i);
            try {
                if (!StringUtils.hasText(item.getMobile())) {
                    throw new BizException("mobile is required");
                }
                boolean duplicate = isDuplicateMobile(null, item.getMobile());
                if (duplicate) {
                    result.setDuplicateCount(result.getDuplicateCount() + 1);
                }
                if (request.getAssignToAdminId() != null) {
                    item.setOwnerAdminId(request.getAssignToAdminId());
                } else if (Integer.valueOf(1).equals(request.getAutoPublicPool())) {
                    item.setOwnerAdminId(null);
                }
                createCustomer(item, currentUser);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (RuntimeException e) {
                result.setFailCount(result.getFailCount() + 1);
                result.getErrors().add("row " + (i + 1) + ": " + e.getMessage());
            }
        }
        return result;
    }

    private LambdaQueryWrapper<CrmCustomer> customerWrapper(CrmCustomerQueryDTO query, CrmCurrentUserDTO currentUser) {
        LambdaQueryWrapper<CrmCustomer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getCustomerName())) {
            wrapper.like(CrmCustomer::getCustomerName, query.getCustomerName().trim());
        }
        if (StringUtils.hasText(query.getMobile())) {
            wrapper.like(CrmCustomer::getMobile, query.getMobile().trim());
        }
        if (StringUtils.hasText(query.getCity())) {
            wrapper.like(CrmCustomer::getCity, query.getCity().trim());
        }
        if (query.getAge() != null) {
            wrapper.eq(CrmCustomer::getAge, query.getAge());
        }
        eqText(wrapper, CrmCustomer::getGender, query.getGender());
        eqText(wrapper, CrmCustomer::getLoanPurpose, query.getLoanPurpose());
        eqText(wrapper, CrmCustomer::getCustomerSource, query.getCustomerSource());
        eqText(wrapper, CrmCustomer::getChannelCode, query.getChannelCode());
        eqText(wrapper, CrmCustomer::getCrmInstCode, query.getCrmInstCode());
        eqText(wrapper, CrmCustomer::getSourceInstCode, query.getSourceInstCode());
        eqText(wrapper, CrmCustomer::getProductName, query.getProductName());
        eqText(wrapper, CrmCustomer::getSourceOrderNo, query.getSourceOrderNo());
        eqText(wrapper, CrmCustomer::getSourceCollisionNo, query.getSourceCollisionNo());
        eqText(wrapper, CrmCustomer::getCustomerStatus, query.getCustomerStatus());
        eqText(wrapper, CrmCustomer::getLoanIntention, query.getLoanIntention());
        if (query.getCrmInstId() != null) {
            wrapper.eq(CrmCustomer::getCrmInstId, query.getCrmInstId());
        }
        if (query.getSourceInstId() != null) {
            wrapper.eq(CrmCustomer::getSourceInstId, query.getSourceInstId());
        }
        if (query.getProductId() != null) {
            wrapper.eq(CrmCustomer::getProductId, query.getProductId());
        }
        if (query.getOwnerAdminId() != null) {
            wrapper.eq(CrmCustomer::getOwnerAdminId, query.getOwnerAdminId());
        }
        if (query.getTeamId() != null) {
            wrapper.eq(CrmCustomer::getTeamId, query.getTeamId());
        }
        if (query.getQualityStar() != null) {
            wrapper.eq(CrmCustomer::getQualityStar, query.getQualityStar());
        }
        range(wrapper, CrmCustomer::getLastFollowTime, query.getLastFollowStart(), query.getLastFollowEnd());
        range(wrapper, CrmCustomer::getNextFollowTime, query.getNextFollowStart(), query.getNextFollowEnd());
        range(wrapper, CrmCustomer::getCreatedAt, query.getCreatedStart(), query.getCreatedEnd());
        if (query.getIsAllocated() != null) {
            wrapper.eq(CrmCustomer::getIsAllocated, query.getIsAllocated());
        }
        if (query.getIsCalled() != null) {
            wrapper.eq(CrmCustomer::getIsCalled, query.getIsCalled());
        }
        if (query.getIsDuplicate() != null) {
            wrapper.eq(CrmCustomer::getIsDuplicate, query.getIsDuplicate());
        }
        if (query.getInPublicPool() != null) {
            wrapper.eq(CrmCustomer::getInPublicPool, query.getInPublicPool());
        }
        applyCustomerDataScope(wrapper, currentUser);
        wrapper.orderByDesc(CrmCustomer::getCreatedAt).orderByDesc(CrmCustomer::getId);
        return wrapper;
    }

    private LambdaQueryWrapper<CrmFollowRecord> followWrapper(CrmFollowRecordQueryDTO query, CrmCurrentUserDTO currentUser) {
        LambdaQueryWrapper<CrmFollowRecord> wrapper = new LambdaQueryWrapper<>();
        if (query.getCustomerId() != null) {
            wrapper.eq(CrmFollowRecord::getCustomerId, query.getCustomerId());
        }
        if (StringUtils.hasText(query.getCustomerName())) {
            wrapper.like(CrmFollowRecord::getCustomerName, query.getCustomerName().trim());
        }
        if (StringUtils.hasText(query.getMobile())) {
            wrapper.like(CrmFollowRecord::getMobile, query.getMobile().trim());
        }
        if (query.getFollowerAdminId() != null) {
            wrapper.eq(CrmFollowRecord::getFollowerAdminId, query.getFollowerAdminId());
        }
        eqText(wrapper, CrmFollowRecord::getFollowMethod, query.getFollowMethod());
        eqText(wrapper, CrmFollowRecord::getFollowResult, query.getFollowResult());
        eqText(wrapper, CrmFollowRecord::getLoanIntention, query.getLoanIntention());
        if (query.getQualityStar() != null) {
            wrapper.eq(CrmFollowRecord::getQualityStar, query.getQualityStar());
        }
        range(wrapper, CrmFollowRecord::getFollowTime, query.getFollowStart(), query.getFollowEnd());
        range(wrapper, CrmFollowRecord::getNextFollowTime, query.getNextFollowStart(), query.getNextFollowEnd());
        if (currentUser.isSupervisor()) {
            wrapper.eq(CrmFollowRecord::getTeamId, currentUser.getTeamId());
        } else if (currentUser.isOperator()) {
            wrapper.eq(CrmFollowRecord::getFollowerAdminId, currentUser.getAdminId());
        }
        wrapper.orderByDesc(CrmFollowRecord::getFollowTime).orderByDesc(CrmFollowRecord::getId);
        return wrapper;
    }

    private void applyCustomerDataScope(LambdaQueryWrapper<CrmCustomer> wrapper, CrmCurrentUserDTO currentUser) {
        if (currentUser == null || currentUser.isAdmin()) {
            return;
        }
        if (currentUser.isSupervisor()) {
            wrapper.eq(CrmCustomer::getTeamId, currentUser.getTeamId());
            return;
        }
        wrapper.eq(CrmCustomer::getOwnerAdminId, currentUser.getAdminId());
    }

    private void fillCustomer(CrmCustomer customer, CrmCustomerSaveDTO request) {
        customer.setCustomerName(trimToNull(request.getCustomerName()));
        customer.setMobile(normalizeMobile(request.getMobile()));
        customer.setMobileMd5(DigestUtil.md5Hex(customer.getMobile()));
        customer.setIdCard(trimToNull(request.getIdCard()));
        customer.setCity(trimToNull(request.getCity()));
        customer.setAge(request.getAge());
        customer.setGender(trimToNull(request.getGender()));
        customer.setOccupation(trimToNull(request.getOccupation()));
        customer.setMonthlyIncome(request.getMonthlyIncome());
        customer.setHasSocialSecurity(request.getHasSocialSecurity());
        customer.setHasHousingFund(request.getHasHousingFund());
        customer.setHasHouse(request.getHasHouse());
        customer.setHasCar(request.getHasCar());
        customer.setSesameScore(request.getSesameScore());
        customer.setCreditCardStatus(trimToNull(request.getCreditCardStatus()));
        customer.setLoanAmount(request.getLoanAmount());
        customer.setLoanPurpose(trimToNull(request.getLoanPurpose()));
        customer.setExpectedTerm(trimToNull(request.getExpectedTerm()));
        customer.setCustomerSource(trimToNull(request.getCustomerSource()));
        customer.setChannelCode(trimToNull(request.getChannelCode()));
        customer.setCrmInstId(request.getCrmInstId());
        customer.setCrmInstCode(trimToNull(request.getCrmInstCode()));
        customer.setCrmInstName(trimToNull(request.getCrmInstName()));
        customer.setSourceInstId(request.getSourceInstId());
        customer.setSourceInstCode(trimToNull(request.getSourceInstCode()));
        customer.setSourceInstName(trimToNull(request.getSourceInstName()));
        customer.setProductId(request.getProductId());
        customer.setProductName(trimToNull(request.getProductName()));
        customer.setSourceOrderNo(trimToNull(request.getSourceOrderNo()));
        customer.setSourceCollisionNo(trimToNull(request.getSourceCollisionNo()));
        customer.setOwnerAdminId(request.getOwnerAdminId());
        customer.setTeamId(request.getTeamId());
        customer.setCustomerStatus(defaultText(request.getCustomerStatus(), "UNFOLLOWED"));
        customer.setLoanIntention(defaultText(request.getLoanIntention(), "UNCONFIRMED"));
        customer.setQualityStar(request.getQualityStar());
        customer.setNextFollowTime(request.getNextFollowTime());
        customer.setIsValid(request.getIsValid());
        customer.setWechatAdded(request.getWechatAdded());
        customer.setNeedRecall(request.getNeedRecall());
        customer.setIsDeal(request.getIsDeal());
        customer.setIsRejected(request.getIsRejected());
        customer.setWagePaymentType(trimToNull(request.getWagePaymentType()));
        customer.setSocialSecurityStatus(trimToNull(request.getSocialSecurityStatus()));
        customer.setHousingFundStatus(trimToNull(request.getHousingFundStatus()));
        customer.setHouseStatus(trimToNull(request.getHouseStatus()));
        customer.setCarStatus(trimToNull(request.getCarStatus()));
        customer.setInsuranceStatus(trimToNull(request.getInsuranceStatus()));
        customer.setCreditStatus(trimToNull(request.getCreditStatus()));
        customer.setHasOverdue(request.getHasOverdue());
        customer.setCurrentDebt(request.getCurrentDebt());
        customer.setHasCreditCard(request.getHasCreditCard());
        customer.setHasOnlineLoan(request.getHasOnlineLoan());
        customer.setAcceptableRate(request.getAcceptableRate());
        customer.setUrgentMoney(request.getUrgentMoney());
        customer.setRemark(trimToNull(request.getRemark()));
    }

    private void applyCustomerDefaults(CrmCustomer customer) {
        customer.setFollowCount(customer.getFollowCount() == null ? 0 : customer.getFollowCount());
        customer.setIsCalled(customer.getIsCalled() == null ? 0 : customer.getIsCalled());
        customer.setIsValid(customer.getIsValid() == null ? 1 : customer.getIsValid());
        customer.setWechatAdded(customer.getWechatAdded() == null ? 0 : customer.getWechatAdded());
        customer.setNeedRecall(customer.getNeedRecall() == null ? 0 : customer.getNeedRecall());
        customer.setIsDeal(customer.getIsDeal() == null ? 0 : customer.getIsDeal());
        customer.setIsRejected(customer.getIsRejected() == null ? 0 : customer.getIsRejected());
        customer.setQualityStar(customer.getQualityStar() == null ? 3 : customer.getQualityStar());
        customer.setIsKeyCustomer(customer.getQualityStar() >= 4 ? 1 : 0);
    }

    private CrmCustomerVO toCustomerVO(CrmCustomer customer, boolean sensitive) {
        CrmCustomerVO vo = new CrmCustomerVO();
        BeanUtils.copyProperties(customer, vo);
        vo.setMobileMasked(maskMobile(customer.getMobile()));
        vo.setIdCardMasked(maskIdCard(customer.getIdCard()));
        if (!sensitive) {
            vo.setMobile(null);
            vo.setIdCard(null);
        }
        return vo;
    }

    private CrmFollowRecordVO toFollowRecordVO(CrmFollowRecord record, boolean sensitive) {
        CrmFollowRecordVO vo = new CrmFollowRecordVO();
        BeanUtils.copyProperties(record, vo);
        vo.setMobileMasked(maskMobile(record.getMobile()));
        if (!sensitive) {
            vo.setMobile(null);
        }
        return vo;
    }

    private CrmEmployeeProfileVO toEmployeeProfileVO(CrmEmployeeProfile profile) {
        CrmEmployeeProfileVO vo = new CrmEmployeeProfileVO();
        BeanUtils.copyProperties(profile, vo);
        CrmTeam team = profile.getTeamId() == null ? null : crmTeamMapper.selectById(profile.getTeamId());
        vo.setTeamName(team == null ? null : team.getTeamName());
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        vo.setCurrentCustomerCount(toInt(crmCustomerMapper.selectCount(new LambdaQueryWrapper<CrmCustomer>().eq(CrmCustomer::getOwnerAdminId, profile.getAdminId()).eq(CrmCustomer::getInPublicPool, 0))));
        vo.setTodayAssignedCount(toInt(crmAssignmentRecordMapper.selectCount(new LambdaQueryWrapper<CrmAssignmentRecord>().eq(CrmAssignmentRecord::getToAdminId, profile.getAdminId()).ge(CrmAssignmentRecord::getAssignedAt, start).lt(CrmAssignmentRecord::getAssignedAt, end))));
        vo.setTodayFollowedCount(toInt(crmFollowRecordMapper.selectCount(new LambdaQueryWrapper<CrmFollowRecord>().eq(CrmFollowRecord::getFollowerAdminId, profile.getAdminId()).ge(CrmFollowRecord::getFollowTime, start).lt(CrmFollowRecord::getFollowTime, end))));
        vo.setDealCount(toInt(crmCustomerMapper.selectCount(new LambdaQueryWrapper<CrmCustomer>().eq(CrmCustomer::getOwnerAdminId, profile.getAdminId()).eq(CrmCustomer::getIsDeal, 1))));
        return vo;
    }

    private void assertCustomerVisible(CrmCustomer customer, CrmCurrentUserDTO currentUser) {
        if (currentUser.isAdmin()) {
            return;
        }
        if (currentUser.isSupervisor() && Objects.equals(customer.getTeamId(), currentUser.getTeamId())) {
            return;
        }
        if (Objects.equals(customer.getOwnerAdminId(), currentUser.getAdminId())) {
            return;
        }
        throw new BizException("no permission to view customer");
    }

    private void assertCustomerWritable(CrmCustomer customer, CrmCurrentUserDTO currentUser) {
        if (currentUser.isAdmin()) {
            return;
        }
        if (currentUser.isSupervisor() && Objects.equals(customer.getTeamId(), currentUser.getTeamId())) {
            return;
        }
        if (Objects.equals(customer.getOwnerAdminId(), currentUser.getAdminId())) {
            return;
        }
        throw new BizException("no permission to operate customer");
    }

    private boolean canSeeSensitive(CrmCustomer customer, CrmCurrentUserDTO currentUser) {
        return currentUser.isAdmin()
                || (currentUser.isSupervisor() && Objects.equals(customer.getTeamId(), currentUser.getTeamId()))
                || Objects.equals(customer.getOwnerAdminId(), currentUser.getAdminId());
    }

    private void fillOwner(CrmCustomer customer, Long adminId) {
        CrmEmployeeProfile profile = findProfile(adminId);
        SysAdmin admin = sysAdminMapper.selectById(adminId);
        if (admin == null && profile == null) {
            throw new BizException("owner not found: " + adminId);
        }
        customer.setOwnerAdminId(adminId);
        customer.setOwnerName(profile != null ? profile.getEmployeeName() : (StringUtils.hasText(admin.getRealName()) ? admin.getRealName() : admin.getUsername()));
        customer.setTeamId(profile == null ? customer.getTeamId() : profile.getTeamId());
    }

    private CrmCustomer requireCustomer(Long id) {
        if (id == null) {
            throw new BizException("customerId is required");
        }
        CrmCustomer customer = crmCustomerMapper.selectById(id);
        if (customer == null) {
            throw new BizException("customer not found: " + id);
        }
        return customer;
    }

    private CrmEmployeeProfile requireActiveProfile(Long adminId) {
        CrmEmployeeProfile profile = findProfile(adminId);
        if (profile == null || !Integer.valueOf(1).equals(profile.getStatus())) {
            throw new BizException("employee profile not active: " + adminId);
        }
        return profile;
    }

    private CrmEmployeeProfile findProfile(Long adminId) {
        if (adminId == null) {
            return null;
        }
        return crmEmployeeProfileMapper.selectOne(new LambdaQueryWrapper<CrmEmployeeProfile>()
                .eq(CrmEmployeeProfile::getAdminId, adminId)
                .last("LIMIT 1"));
    }

    private void writeAssignmentRecord(CrmCustomer customer, CrmEmployeeProfile assignee, CrmCurrentUserDTO currentUser, String mode, String remark) {
        CrmAssignmentRecord record = new CrmAssignmentRecord();
        record.setCustomerId(customer.getId());
        record.setCustomerName(customer.getCustomerName());
        record.setFromAdminId(customer.getOwnerAdminId());
        record.setFromAdminName(customer.getOwnerName());
        record.setToAdminId(assignee.getAdminId());
        record.setToAdminName(assignee.getEmployeeName());
        record.setAssignerAdminId(currentUser.getAdminId());
        record.setAssignerName(currentUser.getRealName());
        record.setAssignMode(mode);
        record.setRemark(remark);
        record.setAssignedAt(LocalDateTime.now());
        crmAssignmentRecordMapper.insert(record);
    }

    private void writePublicPoolRecord(CrmCustomer customer, CrmEmployeeProfile profile, CrmCurrentUserDTO currentUser, String reason, String actionType, String remark) {
        CrmPublicPoolRecord record = new CrmPublicPoolRecord();
        record.setCustomerId(customer.getId());
        record.setCustomerName(customer.getCustomerName());
        record.setPreviousOwnerAdminId(customer.getOwnerAdminId());
        record.setPreviousOwnerName(customer.getOwnerName());
        record.setOperatorAdminId(currentUser.getAdminId());
        record.setOperatorName(currentUser.getRealName());
        record.setReason(reason);
        record.setActionType(actionType);
        record.setRemark(profile == null ? remark : defaultText(remark, profile.getEmployeeName()));
        record.setActionAt(LocalDateTime.now());
        crmPublicPoolRecordMapper.insert(record);
    }

    private boolean isDuplicateMobile(Long id, String mobile) {
        if (!StringUtils.hasText(mobile)) {
            return false;
        }
        LambdaQueryWrapper<CrmCustomer> wrapper = new LambdaQueryWrapper<CrmCustomer>().eq(CrmCustomer::getMobile, mobile.trim());
        if (id != null) {
            wrapper.ne(CrmCustomer::getId, id);
        }
        return crmCustomerMapper.selectCount(wrapper) > 0;
    }

    private int dailyClaimCount(Long adminId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return toInt(crmPublicPoolRecordMapper.selectCount(new LambdaQueryWrapper<CrmPublicPoolRecord>()
                .eq(CrmPublicPoolRecord::getOperatorAdminId, adminId)
                .eq(CrmPublicPoolRecord::getActionType, "CLAIM")
                .ge(CrmPublicPoolRecord::getActionAt, start)
                .lt(CrmPublicPoolRecord::getActionAt, start.plusDays(1))));
    }

    private long countCustomers(LambdaQueryWrapper<CrmCustomer> wrapper, CrmCurrentUserDTO currentUser) {
        applyCustomerDataScope(wrapper, currentUser);
        return crmCustomerMapper.selectCount(wrapper);
    }

    private long countFollows(LambdaQueryWrapper<CrmFollowRecord> wrapper, CrmCurrentUserDTO currentUser) {
        if (currentUser != null) {
            if (currentUser.isSupervisor()) {
                wrapper.eq(CrmFollowRecord::getTeamId, currentUser.getTeamId());
            } else if (currentUser.isOperator()) {
                wrapper.eq(CrmFollowRecord::getFollowerAdminId, currentUser.getAdminId());
            }
        }
        return crmFollowRecordMapper.selectCount(wrapper);
    }

    private List<CrmCustomerVO> limitedCustomers(LambdaQueryWrapper<CrmCustomer> wrapper, CrmCurrentUserDTO currentUser, int limit) {
        applyCustomerDataScope(wrapper, currentUser);
        wrapper.last("LIMIT " + limit);
        return crmCustomerMapper.selectList(wrapper).stream()
                .map(customer -> toCustomerVO(customer, canSeeSensitive(customer, currentUser)))
                .collect(Collectors.toList());
    }

    private List<CrmFollowRecordVO> limitedFollows(LambdaQueryWrapper<CrmFollowRecord> wrapper, CrmCurrentUserDTO currentUser, int limit) {
        if (currentUser.isSupervisor()) {
            wrapper.eq(CrmFollowRecord::getTeamId, currentUser.getTeamId());
        } else if (currentUser.isOperator()) {
            wrapper.eq(CrmFollowRecord::getFollowerAdminId, currentUser.getAdminId());
        }
        wrapper.last("LIMIT " + limit);
        return crmFollowRecordMapper.selectList(wrapper).stream()
                .map(record -> toFollowRecordVO(record, currentUser.isAdmin() || Objects.equals(record.getFollowerAdminId(), currentUser.getAdminId())))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> groupCustomer(String column, CrmCurrentUserDTO currentUser) {
        QueryWrapper<CrmCustomer> wrapper = new QueryWrapper<>();
        wrapper.select(column + " AS name", "COUNT(1) AS value").groupBy(column);
        if (currentUser != null) {
            if (currentUser.isSupervisor()) {
                wrapper.eq("team_id", currentUser.getTeamId());
            } else if (currentUser.isOperator()) {
                wrapper.eq("owner_admin_id", currentUser.getAdminId());
            }
        }
        return sanitizeGroupRows(crmCustomerMapper.selectMaps(wrapper));
    }

    private List<Map<String, Object>> groupFollow(String column, CrmCurrentUserDTO currentUser) {
        QueryWrapper<CrmFollowRecord> wrapper = new QueryWrapper<>();
        wrapper.select(column + " AS name", "COUNT(1) AS value").groupBy(column).orderByDesc("value").last("LIMIT 10");
        if (currentUser.isSupervisor()) {
            wrapper.eq("team_id", currentUser.getTeamId());
        } else if (currentUser.isOperator()) {
            wrapper.eq("follower_admin_id", currentUser.getAdminId());
        }
        return sanitizeGroupRows(crmFollowRecordMapper.selectMaps(wrapper));
    }

    private List<Map<String, Object>> sanitizeGroupRows(List<Map<String, Object>> rows) {
        if (rows == null) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            Object name = firstPresent(row, "name", "NAME");
            Object value = firstPresent(row, "value", "VALUE");
            map.put("name", name == null ? "UNKNOWN" : name);
            map.put("value", value == null ? 0 : value);
            return map;
        }).collect(Collectors.toList());
    }

    private Object firstPresent(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
        }
        return null;
    }

    private CrmCurrentUserDTO cloneForTeam(CrmCurrentUserDTO currentUser, Long teamId) {
        CrmCurrentUserDTO dto = new CrmCurrentUserDTO();
        BeanUtils.copyProperties(currentUser, dto);
        dto.setTeamId(teamId);
        if (currentUser.isAdmin()) {
            dto.setRole("SUPERVISOR");
        }
        return dto;
    }

    private boolean isPositiveIntention(String loanIntention) {
        return "MEDIUM".equals(loanIntention) || "STRONG".equals(loanIntention) || "APPLIED".equals(loanIntention) || "DEAL".equals(loanIntention);
    }

    private String normalizeMobile(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            throw new BizException("mobile is required");
        }
        return mobile.trim();
    }

    private String maskMobile(String mobile) {
        if (!StringUtils.hasText(mobile) || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    private String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first.trim() : second;
    }

    private long pageCurrent(Long current) {
        return current == null || current < 1 ? 1L : current;
    }

    private long pageSize(Long size) {
        return size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
    }

    private int toInt(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private <T> void eqText(LambdaQueryWrapper<T> wrapper, com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, String> column, String value) {
        if (StringUtils.hasText(value)) {
            wrapper.eq(column, value.trim());
        }
    }

    private <T> void range(LambdaQueryWrapper<T> wrapper, com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, LocalDateTime> column, LocalDateTime start, LocalDateTime end) {
        if (start != null) {
            wrapper.ge(column, start);
        }
        if (end != null) {
            wrapper.le(column, end);
        }
    }
}
