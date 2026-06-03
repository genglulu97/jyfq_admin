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
public class CrmSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CrmSchemaInitializer.class);

    private final DataSource dataSource;

    public CrmSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            createCrmCustomerIfMissing(connection);
            createCrmInstitutionConfigIfMissing(connection);
            addColumnIfMissing(connection, "crm_institution_config", "platform_inst_id",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `platform_inst_id` BIGINT NULL AFTER `inst_name`");
            addColumnIfMissing(connection, "crm_institution_config", "platform_inst_code",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `platform_inst_code` VARCHAR(64) NULL AFTER `platform_inst_id`");
            addColumnIfMissing(connection, "crm_institution_config", "platform_inst_name",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `platform_inst_name` VARCHAR(128) NULL AFTER `platform_inst_code`");
            addColumnIfMissing(connection, "crm_institution_config", "crm_inst_id",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `crm_inst_id` BIGINT NULL AFTER `platform_inst_name`");
            addColumnIfMissing(connection, "crm_institution_config", "crm_inst_code",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `crm_inst_code` VARCHAR(64) NULL AFTER `crm_inst_id`");
            addColumnIfMissing(connection, "crm_institution_config", "crm_inst_name",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `crm_inst_name` VARCHAR(128) NULL AFTER `crm_inst_code`");
            addColumnIfMissing(connection, "crm_institution_config", "crm_org_id",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `crm_org_id` VARCHAR(64) NULL AFTER `crm_inst_name`");
            addColumnIfMissing(connection, "crm_institution_config", "crm_org_name",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `crm_org_name` VARCHAR(128) NULL AFTER `crm_org_id`");
            addColumnIfMissing(connection, "crm_institution_config", "crm_org_code",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `crm_org_code` VARCHAR(64) NULL AFTER `crm_org_name`");
            addColumnIfMissing(connection, "crm_institution_config", "auto_push",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `auto_push` TINYINT NOT NULL DEFAULT 1 AFTER `crm_org_code`");
            addColumnIfMissing(connection, "crm_institution_config", "crm_admin_name",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `crm_admin_name` VARCHAR(64) NULL AFTER `owner_name`");
            addColumnIfMissing(connection, "crm_institution_config", "crm_admin_phone",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `crm_admin_phone` VARCHAR(32) NULL AFTER `crm_admin_name`");
            addColumnIfMissing(connection, "crm_institution_config", "crm_admin_email",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `crm_admin_email` VARCHAR(128) NULL AFTER `crm_admin_phone`");
            addColumnIfMissing(connection, "crm_institution_config", "crm_admin_role",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `crm_admin_role` VARCHAR(64) NULL AFTER `crm_admin_email`");
            addColumnIfMissing(connection, "crm_institution_config", "crm_admin_account",
                    "ALTER TABLE `crm_institution_config` ADD COLUMN `crm_admin_account` VARCHAR(64) NULL AFTER `crm_admin_role`");
            addIndexIfMissing(connection, "crm_institution_config", "idx_platform_inst_id",
                    "ALTER TABLE `crm_institution_config` ADD INDEX `idx_platform_inst_id` (`platform_inst_id`)");
            addIndexIfMissing(connection, "crm_institution_config", "idx_crm_inst_id",
                    "ALTER TABLE `crm_institution_config` ADD INDEX `idx_crm_inst_id` (`crm_inst_id`)");
            backfillCrmInstitutionSnapshot(connection);
        }
    }

    private void createCrmCustomerIfMissing(Connection connection) throws Exception {
        if (hasTable(connection, "crm_customer")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE `crm_customer` ("
                    + "`id` BIGINT PRIMARY KEY AUTO_INCREMENT,"
                    + "`customer_name` VARCHAR(64) NOT NULL,"
                    + "`mobile` VARCHAR(32) NOT NULL,"
                    + "`mobile_md5` VARCHAR(64) NULL,"
                    + "`crm_inst_id` BIGINT NULL,"
                    + "`crm_inst_code` VARCHAR(64) NULL,"
                    + "`crm_inst_name` VARCHAR(128) NULL,"
                    + "`source_inst_id` BIGINT NULL,"
                    + "`source_inst_code` VARCHAR(64) NULL,"
                    + "`source_inst_name` VARCHAR(128) NULL,"
                    + "`product_id` BIGINT NULL,"
                    + "`product_name` VARCHAR(128) NULL,"
                    + "`source_order_no` VARCHAR(64) NULL,"
                    + "`source_collision_no` VARCHAR(64) NULL,"
                    + "`id_card` VARCHAR(64) NULL,"
                    + "`city` VARCHAR(64) NULL,"
                    + "`age` INT NULL,"
                    + "`gender` VARCHAR(16) NULL,"
                    + "`occupation` VARCHAR(64) NULL,"
                    + "`monthly_income` DECIMAL(12,2) NULL,"
                    + "`has_social_security` TINYINT NULL,"
                    + "`has_housing_fund` TINYINT NULL,"
                    + "`has_house` TINYINT NULL,"
                    + "`has_car` TINYINT NULL,"
                    + "`sesame_score` INT NULL,"
                    + "`credit_card_status` VARCHAR(64) NULL,"
                    + "`loan_amount` DECIMAL(14,2) NULL,"
                    + "`loan_purpose` VARCHAR(128) NULL,"
                    + "`expected_term` VARCHAR(64) NULL,"
                    + "`customer_source` VARCHAR(64) NULL,"
                    + "`channel_code` VARCHAR(64) NULL,"
                    + "`owner_admin_id` BIGINT NULL,"
                    + "`owner_name` VARCHAR(64) NULL,"
                    + "`team_id` BIGINT NULL,"
                    + "`customer_status` VARCHAR(64) NOT NULL DEFAULT 'UNFOLLOWED',"
                    + "`loan_intention` VARCHAR(64) NOT NULL DEFAULT 'UNCONFIRMED',"
                    + "`quality_star` INT NOT NULL DEFAULT 3,"
                    + "`follow_count` INT NOT NULL DEFAULT 0,"
                    + "`last_follow_time` DATETIME NULL,"
                    + "`next_follow_time` DATETIME NULL,"
                    + "`last_follow_remark` VARCHAR(1024) NULL,"
                    + "`is_allocated` TINYINT NOT NULL DEFAULT 0,"
                    + "`is_called` TINYINT NOT NULL DEFAULT 0,"
                    + "`is_duplicate` TINYINT NOT NULL DEFAULT 0,"
                    + "`is_valid` TINYINT NOT NULL DEFAULT 1,"
                    + "`wechat_added` TINYINT NOT NULL DEFAULT 0,"
                    + "`need_recall` TINYINT NOT NULL DEFAULT 0,"
                    + "`is_deal` TINYINT NOT NULL DEFAULT 0,"
                    + "`is_rejected` TINYINT NOT NULL DEFAULT 0,"
                    + "`is_key_customer` TINYINT NOT NULL DEFAULT 0,"
                    + "`in_public_pool` TINYINT NOT NULL DEFAULT 1,"
                    + "`public_pool_reason` VARCHAR(64) NULL,"
                    + "`wage_payment_type` VARCHAR(64) NULL,"
                    + "`social_security_status` VARCHAR(64) NULL,"
                    + "`housing_fund_status` VARCHAR(64) NULL,"
                    + "`house_status` VARCHAR(64) NULL,"
                    + "`car_status` VARCHAR(64) NULL,"
                    + "`insurance_status` VARCHAR(64) NULL,"
                    + "`credit_status` VARCHAR(64) NULL,"
                    + "`has_overdue` TINYINT NULL,"
                    + "`current_debt` DECIMAL(14,2) NULL,"
                    + "`has_credit_card` TINYINT NULL,"
                    + "`has_online_loan` TINYINT NULL,"
                    + "`acceptable_rate` DECIMAL(8,4) NULL,"
                    + "`urgent_money` TINYINT NULL,"
                    + "`remark` VARCHAR(1024) NULL,"
                    + "`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "`create_by` VARCHAR(64) NULL,"
                    + "`updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                    + "`update_by` VARCHAR(64) NULL,"
                    + "KEY `idx_mobile` (`mobile`),"
                    + "KEY `idx_mobile_md5` (`mobile_md5`),"
                    + "KEY `idx_crm_inst_mobile_md5` (`crm_inst_id`, `mobile_md5`),"
                    + "KEY `idx_crm_inst_id` (`crm_inst_id`),"
                    + "KEY `idx_source_inst_id` (`source_inst_id`),"
                    + "KEY `idx_product_id` (`product_id`),"
                    + "KEY `idx_source_order_no` (`source_order_no`),"
                    + "KEY `idx_source_collision_no` (`source_collision_no`),"
                    + "KEY `idx_owner_admin_id` (`owner_admin_id`),"
                    + "KEY `idx_team_id` (`team_id`),"
                    + "KEY `idx_status` (`customer_status`),"
                    + "KEY `idx_intention` (`loan_intention`),"
                    + "KEY `idx_quality_star` (`quality_star`),"
                    + "KEY `idx_next_follow_time` (`next_follow_time`),"
                    + "KEY `idx_public_pool` (`in_public_pool`),"
                    + "KEY `idx_created_at` (`created_at`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM customer lead'");
            log.info("[SCHEMA] created crm_customer");
        }
    }

    private void createCrmInstitutionConfigIfMissing(Connection connection) throws Exception {
        if (hasTable(connection, "crm_institution_config")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE `crm_institution_config` ("
                    + "`id` BIGINT PRIMARY KEY AUTO_INCREMENT,"
                    + "`inst_id` BIGINT NOT NULL,"
                    + "`inst_code` VARCHAR(64) NOT NULL,"
                    + "`inst_name` VARCHAR(128) NOT NULL,"
                    + "`platform_inst_id` BIGINT NULL,"
                    + "`platform_inst_code` VARCHAR(64) NULL,"
                    + "`platform_inst_name` VARCHAR(128) NULL,"
                    + "`crm_inst_id` BIGINT NULL,"
                    + "`crm_inst_code` VARCHAR(64) NULL,"
                    + "`crm_inst_name` VARCHAR(128) NULL,"
                    + "`crm_org_id` VARCHAR(64) NULL,"
                    + "`crm_org_name` VARCHAR(128) NULL,"
                    + "`crm_org_code` VARCHAR(64) NULL,"
                    + "`auto_push` TINYINT NOT NULL DEFAULT 1,"
                    + "`auto_assign` TINYINT NOT NULL DEFAULT 0,"
                    + "`owner_admin_id` BIGINT NULL,"
                    + "`owner_name` VARCHAR(64) NULL,"
                    + "`crm_admin_name` VARCHAR(64) NULL,"
                    + "`crm_admin_phone` VARCHAR(32) NULL,"
                    + "`crm_admin_email` VARCHAR(128) NULL,"
                    + "`crm_admin_role` VARCHAR(64) NULL,"
                    + "`crm_admin_account` VARCHAR(64) NULL,"
                    + "`team_id` BIGINT NULL,"
                    + "`customer_source` VARCHAR(64) NOT NULL DEFAULT 'CRM_API',"
                    + "`status` TINYINT NOT NULL DEFAULT 1,"
                    + "`remark` VARCHAR(512) NULL,"
                    + "`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "`create_by` VARCHAR(64) NULL,"
                    + "`updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                    + "`update_by` VARCHAR(64) NULL,"
                    + "UNIQUE KEY `uk_inst_id` (`inst_id`),"
                    + "KEY `idx_inst_code` (`inst_code`),"
                    + "KEY `idx_platform_inst_id` (`platform_inst_id`),"
                    + "KEY `idx_crm_inst_id` (`crm_inst_id`),"
                    + "KEY `idx_owner_admin_id` (`owner_admin_id`),"
                    + "KEY `idx_team_id` (`team_id`),"
                    + "KEY `idx_status` (`status`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM institution binding config'");
            log.info("[SCHEMA] created crm_institution_config");
        }
    }

    private void addColumnIfMissing(Connection connection, String tableName, String columnName, String ddl) throws Exception {
        if (hasColumn(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(ddl);
            log.info("[SCHEMA] added {}.{}", tableName, columnName);
        }
    }

    private void addIndexIfMissing(Connection connection, String tableName, String indexName, String ddl) throws Exception {
        if (hasIndex(connection, tableName, indexName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(ddl);
            log.info("[SCHEMA] added {}.{}", tableName, indexName);
        }
    }

    private void backfillCrmInstitutionSnapshot(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE `crm_institution_config` SET "
                    + "`crm_inst_id` = `inst_id`, "
                    + "`crm_inst_code` = `inst_code`, "
                    + "`crm_inst_name` = `inst_name` "
                    + "WHERE `crm_inst_id` IS NULL");
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

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws Exception {
        String sql = "SELECT COUNT(1) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean hasIndex(Connection connection, String tableName, String indexName) throws Exception {
        String sql = "SELECT COUNT(1) FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }
}
