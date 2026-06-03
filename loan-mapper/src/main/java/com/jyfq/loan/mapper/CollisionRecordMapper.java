package com.jyfq.loan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jyfq.loan.model.dto.CollisionLogQueryDTO;
import com.jyfq.loan.model.entity.CollisionRecord;
import com.jyfq.loan.model.vo.CollisionLogRecordRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Collision record mapper.
 */
@Mapper
public interface CollisionRecordMapper extends BaseMapper<CollisionRecord> {

    Page<CollisionLogRecordRow> selectCollisionLogPage(Page<CollisionLogRecordRow> page,
                                                       @Param("query") CollisionLogQueryDTO query);
}
