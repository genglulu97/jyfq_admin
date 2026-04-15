package com.jyfq.loan.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger / Knife4j 接口文档配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("贷超分发平台 - APP API 接口文档")
                        .version("1.0.0")
                        .description("包含上游渠道对接、进件匹配、竞价撞库及推单核心功能。")
                        .contact(new Contact().name("JYFQ-Tech").email("tech@jyfq.com"))
                        .license(new License().name("Apache 2.0")));
    }
}
