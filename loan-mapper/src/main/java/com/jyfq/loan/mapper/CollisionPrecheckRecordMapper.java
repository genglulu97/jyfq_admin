package com.jyfq.loan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jyfq.loan.model.entity.CollisionPrecheckRecord;
import com.jyfq.loan.model.vo.CollisionLogRecordRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Collision pre-check detail mapper.
 */
@Mapper
public interface CollisionPrecheckRecordMapper extends BaseMapper<CollisionPrecheckRecord> {

    List<CollisionLogRecordRow> selectCollisionLogDetails(@Param("collisionNo") String collisionNo);
}
