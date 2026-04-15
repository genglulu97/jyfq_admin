package com.jyfq.loan.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * H5 进件 / 渠道查询 API 启动类
 */
@EnableAsync
@SpringBootApplication
@ComponentScan(basePackages = "com.jyfq.loan")
@MapperScan("com.jyfq.loan.mapper")
public class LoanAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanAppApplication.class, args);
    }
}
