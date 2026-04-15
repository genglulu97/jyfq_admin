package com.jyfq.loan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jyfq.loan.model.entity.PushRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 推单记录 Mapper
 */
@Mapper
public interface PushRecordMapper extends BaseMapper<PushRecord> {

    /**
     * 根据订单号和机构编码更新推单状态
     */
    int updateStatus(@Param("orderNo") String orderNo,
                     @Param("instCode") String instCode,
                     @Param("pushStatus") int pushStatus);
}
