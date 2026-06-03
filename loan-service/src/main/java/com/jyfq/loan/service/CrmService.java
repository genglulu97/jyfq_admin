package com.jyfq.loan.service;

import com.jyfq.loan.common.result.PageResult;
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
import com.jyfq.loan.model.vo.CrmCustomerDetailVO;
import com.jyfq.loan.model.vo.CrmCustomerVO;
import com.jyfq.loan.model.vo.CrmDashboardVO;
import com.jyfq.loan.model.vo.CrmEmployeeProfileVO;
import com.jyfq.loan.model.vo.CrmFollowRecordVO;
import com.jyfq.loan.model.vo.CrmImportResultVO;
import com.jyfq.loan.model.vo.CrmTeamSummaryVO;

import java.util.List;

public interface CrmService {

    PageResult<CrmCustomerVO> pageCustomers(CrmCustomerQueryDTO query, CrmCurrentUserDTO currentUser);

    PageResult<CrmCustomerVO> pageMyCustomers(CrmCustomerQueryDTO query, CrmCurrentUserDTO currentUser);

    PageResult<CrmCustomerVO> pagePublicPool(CrmPublicPoolQueryDTO query, CrmCurrentUserDTO currentUser);

    CrmCustomerDetailVO customerDetail(Long id, CrmCurrentUserDTO currentUser);

    Long createCustomer(CrmCustomerSaveDTO request, CrmCurrentUserDTO currentUser);

    void updateCustomer(Long id, CrmCustomerSaveDTO request, CrmCurrentUserDTO currentUser);

    void deleteCustomer(Long id, CrmCurrentUserDTO currentUser);

    void assignCustomers(CrmCustomerAssignDTO request, CrmCurrentUserDTO currentUser);

    void reclaimCustomers(List<Long> customerIds, String reason, CrmCurrentUserDTO currentUser);

    void claimCustomer(Long customerId, CrmCurrentUserDTO currentUser);

    Long addFollowRecord(CrmFollowRecordSaveDTO request, CrmCurrentUserDTO currentUser);

    PageResult<CrmFollowRecordVO> pageFollowRecords(CrmFollowRecordQueryDTO query, CrmCurrentUserDTO currentUser);

    CrmDashboardVO dashboard(CrmCurrentUserDTO currentUser);

    CrmTeamSummaryVO teamSummary(Long teamId, CrmCurrentUserDTO currentUser);

    PageResult<CrmEmployeeProfileVO> pageEmployeeProfiles(CrmEmployeeProfileQueryDTO query, CrmCurrentUserDTO currentUser);

    Long saveEmployeeProfile(CrmEmployeeProfileSaveDTO request, CrmCurrentUserDTO currentUser);

    CrmImportResultVO batchImportCustomers(CrmCustomerBatchImportDTO request, CrmCurrentUserDTO currentUser);
}
