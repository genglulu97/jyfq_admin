package com.jyfq.loan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jyfq.loan.model.entity.ApplyOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 进件主表 Mapper
 */
@Mapper
public interface ApplyOrderMapper extends BaseMapper<ApplyOrder> {
}
