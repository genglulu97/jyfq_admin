package com.jyfq.loan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jyfq.loan.model.entity.DeductionRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 计费扣费 Mapper
 */
@Mapper
public interface DeductionRecordMapper extends BaseMapper<DeductionRecord> {
}
