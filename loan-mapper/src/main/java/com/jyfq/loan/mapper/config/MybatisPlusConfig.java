package com.jyfq.loan.mapper.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.jyfq.loan.common.util.AuditOperatorUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 公共配置（分页插件 + 自动填充）
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 自动填充 createdAt / updatedAt
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                String operator = resolveOperator(metaObject);
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "createBy", String.class, operator);
                this.strictInsertFill(metaObject, "updateBy", String.class, operator);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                String operator = resolveOperator(metaObject);
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                this.strictUpdateFill(metaObject, "updateBy", String.class, operator);
            }

            private String resolveOperator(MetaObject metaObject) {
                Object operatorName = getFieldValByName("operatorName", metaObject);
                if (operatorName instanceof String value && StringUtils.hasText(value)) {
                    return value.trim();
                }
                return AuditOperatorUtil.currentOperator();
            }
        };
    }
}
