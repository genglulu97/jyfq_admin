package com.jyfq.loan.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 管理后台 API 启动类
 */
@EnableAsync
@SpringBootApplication
@ComponentScan(basePackages = "com.jyfq.loan")
@MapperScan("com.jyfq.loan.mapper")
public class LoanAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanAdminApplication.class, args);
    }
}
