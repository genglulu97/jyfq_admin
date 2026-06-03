package com.jyfq.loan.service;

import com.jyfq.loan.common.result.PageResult;
import com.jyfq.loan.model.dto.CollisionLogQueryDTO;
import com.jyfq.loan.model.vo.CollisionLogDetailVO;
import com.jyfq.loan.model.vo.CollisionLogListVO;

import java.util.List;

/**
 * Admin collision log query service.
 */
public interface AdminCollisionLogService {

    PageResult<CollisionLogListVO> pageCollisionLogs(CollisionLogQueryDTO query);

    List<CollisionLogDetailVO> listCollisionLogDetails(String collisionNo);
}
