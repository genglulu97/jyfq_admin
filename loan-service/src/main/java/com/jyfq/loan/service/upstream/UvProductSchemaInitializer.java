package com.jyfq.loan.service.upstream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class UvProductSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UvProductSchemaInitializer.class);

    private final DataSource dataSource;

    public UvProductSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (hasTable(connection, "uv_product")) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE `uv_product` ("
                        + "`id` BIGINT PRIMARY KEY AUTO_INCREMENT,"
                        + "`name` VARCHAR(128) NOT NULL COMMENT 'Product name',"
                        + "`logo` VARCHAR(1024) DEFAULT NULL COMMENT 'Product logo URL',"
                        + "`status` VARCHAR(16) NOT NULL DEFAULT '上架' COMMENT '上架 / 下架',"
                        + "`position` VARCHAR(16) DEFAULT NULL COMMENT '头部 / 列表',"
                        + "`loan_type` VARCHAR(64) DEFAULT NULL COMMENT 'Loan type display text',"
                        + "`min_amount` INT DEFAULT NULL COMMENT 'Minimum loan amount',"
                        + "`max_amount` INT DEFAULT NULL COMMENT 'Maximum loan amount',"
                        + "`rate` VARCHAR(64) DEFAULT NULL COMMENT 'Rate display text',"
                        + "`term` VARCHAR(64) DEFAULT NULL COMMENT 'Term display text',"
                        + "`weight` INT NOT NULL DEFAULT 0 COMMENT 'Display weight, higher first',"
                        + "`price` DECIMAL(10,2) DEFAULT NULL COMMENT 'UV price',"
                        + "`uv_threshold` INT DEFAULT NULL COMMENT 'UV threshold',"
                        + "`badge` VARCHAR(64) DEFAULT NULL COMMENT 'Product badge',"
                        + "`is_joint` VARCHAR(8) NOT NULL DEFAULT '否' COMMENT '是否联登：是 / 否',"
                        + "`apply_url` VARCHAR(1024) DEFAULT NULL COMMENT 'Apply URL',"
                        + "`joint_channel` VARCHAR(128) DEFAULT NULL COMMENT 'Joint login channel',"
                        + "`joint_key` VARCHAR(255) DEFAULT NULL COMMENT 'Joint login key',"
                        + "`joint_check_url` VARCHAR(1024) DEFAULT NULL COMMENT 'Joint check URL',"
                        + "`joint_login_url` VARCHAR(1024) DEFAULT NULL COMMENT 'Joint login URL',"
                        + "`joint_reg_agreement` VARCHAR(1024) DEFAULT NULL COMMENT 'Joint registration agreement',"
                        + "`auto_time_start` DATETIME DEFAULT NULL COMMENT 'Auto online start time',"
                        + "`auto_time_end` DATETIME DEFAULT NULL COMMENT 'Auto online end time',"
                        + "`auto_offline_time` DATETIME DEFAULT NULL COMMENT 'Auto offline time',"
                        + "`assoc_inst` VARCHAR(128) DEFAULT NULL COMMENT 'Associated institution',"
                        + "`spec_channels` VARCHAR(512) DEFAULT NULL COMMENT 'Specified channels',"
                        + "`min_age` INT DEFAULT NULL COMMENT 'Minimum age',"
                        + "`max_age` INT DEFAULT NULL COMMENT 'Maximum age',"
                        + "`block_provinces` VARCHAR(512) DEFAULT NULL COMMENT 'Blocked provinces',"
                        + "`block_cities` VARCHAR(512) DEFAULT NULL COMMENT 'Blocked cities',"
                        + "`target_regions` VARCHAR(512) DEFAULT NULL COMMENT 'Target regions',"
                        + "`apply_count` INT NOT NULL DEFAULT 0 COMMENT 'Apply count',"
                        + "`zhima` JSON DEFAULT NULL COMMENT 'Zhima options',"
                        + "`house` JSON DEFAULT NULL COMMENT 'House options',"
                        + "`car` JSON DEFAULT NULL COMMENT 'Car options',"
                        + "`gongjijin` JSON DEFAULT NULL COMMENT 'Provident fund options',"
                        + "`job` JSON DEFAULT NULL COMMENT 'Job options',"
                        + "`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "`create_by` VARCHAR(64) DEFAULT NULL,"
                        + "`updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                        + "`update_by` VARCHAR(64) DEFAULT NULL,"
                        + "INDEX `idx_uv_product_status_weight` (`status`, `weight`, `id`),"
                        + "INDEX `idx_uv_product_created_at` (`created_at`),"
                        + "INDEX `idx_uv_product_name` (`name`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UV product configuration'");
                log.info("[SCHEMA] created uv_product");
            }
        }
    }

    private boolean hasTable(Connection connection, String tableName) throws Exception {
        String sql = "SELECT COUNT(1) FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }
}
