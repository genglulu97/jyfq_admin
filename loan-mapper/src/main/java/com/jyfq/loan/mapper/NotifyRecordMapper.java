package com.jyfq.loan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jyfq.loan.model.entity.NotifyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 回调流水 Mapper
 */
@Mapper
public interface NotifyRecordMapper extends BaseMapper<NotifyRecord> {

    /**
     * 幂等检查：判断该回调是否已处理过
     */
    boolean existsByInstAndNo(@Param("instCode") String instCode,
                              @Param("notifyNo") String notifyNo);
}
