package com.jyfq.loan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jyfq.loan.model.entity.InstitutionProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 机构产品 Mapper
 */
@Mapper
public interface InstitutionProductMapper extends BaseMapper<InstitutionProduct> {

    /**
     * 根据城市、年龄、金额匹配可推送的产品列表（按优先级排序）
     */
    List<InstitutionProduct> matchProducts(@Param("cityCode") String cityCode,
                                           @Param("age") Integer age,
                                           @Param("amount") Integer amount);
}
