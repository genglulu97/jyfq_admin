-- =====================================================
-- loan-platform bootstrap schema
-- Database: loan_platform
-- Production bootstrap script
-- =====================================================

CREATE DATABASE IF NOT EXISTS `loan_platform`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `loan_platform`;

CREATE TABLE IF NOT EXISTS `channel` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `channel_id` VARCHAR(64) DEFAULT NULL COMMENT 'Business channel id',
  `channel_code` VARCHAR(32) NOT NULL UNIQUE COMMENT 'Channel code',
  `channel_name` VARCHAR(64) NOT NULL COMMENT 'Channel name',
  `channel_type` VARCHAR(64) DEFAULT NULL COMMENT 'Channel type',
  `business_owner` VARCHAR(64) DEFAULT NULL COMMENT 'Business owner',
  `daily_quota` INT NOT NULL DEFAULT 10000 COMMENT 'Daily quota',
  `normal_recommend` TINYINT NOT NULL DEFAULT 0 COMMENT 'Normal recommend flag',
  `display_product_count` INT NOT NULL DEFAULT 1 COMMENT 'Display product count',
  `actual_push_count` INT NOT NULL DEFAULT 1 COMMENT 'Actual push count',
  `method_name` VARCHAR(64) DEFAULT NULL COMMENT 'Method name',
  `app_key` VARCHAR(64) NOT NULL COMMENT 'Encryption/signing key',
  `ip_whitelist` VARCHAR(512) DEFAULT NULL COMMENT 'Comma-separated IP whitelist',
  `callback_url` VARCHAR(255) DEFAULT NULL COMMENT 'Callback URL',
  `settlement_mode` VARCHAR(32) DEFAULT NULL COMMENT 'Settlement mode',
  `ext_json` TEXT DEFAULT NULL COMMENT 'Extension JSON',
  `fee_rate` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'Settlement amount/rate',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT 'Remark',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_channel_id` (`channel_id`),
  INDEX `idx_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Upstream channel config';

CREATE TABLE IF NOT EXISTS `institution` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `inst_code` VARCHAR(32) NOT NULL UNIQUE COMMENT 'Institution code',
  `inst_name` VARCHAR(64) NOT NULL COMMENT 'Institution name',
  `merchant_alias` VARCHAR(64) DEFAULT NULL COMMENT 'Merchant alias',
  `merchant_type` VARCHAR(32) DEFAULT NULL COMMENT 'Merchant type',
  `api_push_url` VARCHAR(256) NOT NULL COMMENT 'Push API URL',
  `api_notify_url` VARCHAR(256) DEFAULT NULL COMMENT 'Notify callback URL',
  `app_key` VARCHAR(128) NOT NULL COMMENT 'Shared secret',
  `rsa_public_key` TEXT DEFAULT NULL COMMENT 'RSA public key',
  `encrypt_type` VARCHAR(16) NOT NULL DEFAULT 'AES' COMMENT 'AES/RSA',
  `notify_encrypt_type` VARCHAR(16) NOT NULL DEFAULT 'PLAIN' COMMENT 'Notify encryption mode',
  `timeout_ms` INT NOT NULL DEFAULT 3000 COMMENT 'Adapter timeout in ms',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  `open_cities` VARCHAR(255) DEFAULT NULL COMMENT 'Open cities',
  `admin_phone` VARCHAR(20) DEFAULT NULL COMMENT 'Admin phone',
  `admin_name` VARCHAR(32) DEFAULT NULL COMMENT 'Admin name',
  `admin_role` VARCHAR(32) DEFAULT NULL COMMENT 'Admin role',
  `sms_notify` TINYINT DEFAULT 0 COMMENT 'SMS notify enabled',
  `user_status` TINYINT DEFAULT 1 COMMENT 'User status',
  `crm_auto_assign` TINYINT DEFAULT 0 COMMENT 'CRM auto assign',
  `api_merchant` TINYINT DEFAULT 1 COMMENT 'API merchant flag',
  `api_method_name` VARCHAR(64) DEFAULT NULL COMMENT 'API method name',
  `specified_channel` VARCHAR(64) DEFAULT NULL COMMENT 'Specified channel',
  `excluded_channels` VARCHAR(255) DEFAULT NULL COMMENT 'Excluded channels',
  `account_balance` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Merchant balance',
  `recharge_total` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Recharge total',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT 'Remark',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_inst_code` (`inst_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Downstream institution config';

CREATE TABLE IF NOT EXISTS `institution_product` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `inst_id` BIGINT NOT NULL COMMENT 'Institution ID',
  `product_name` VARCHAR(64) NOT NULL COMMENT 'Product name',
  `product_icon` VARCHAR(255) DEFAULT NULL COMMENT 'Product icon',
  `min_age` TINYINT NOT NULL DEFAULT 18 COMMENT 'Minimum age',
  `max_age` TINYINT NOT NULL DEFAULT 60 COMMENT 'Maximum age',
  `min_amount` INT NOT NULL DEFAULT 1000 COMMENT 'Minimum amount',
  `max_amount` INT NOT NULL DEFAULT 200000 COMMENT 'Maximum amount',
  `rate` DECIMAL(10,2) DEFAULT NULL COMMENT 'Product rate',
  `period` INT DEFAULT NULL COMMENT 'Product period',
  `protocol_url` VARCHAR(255) DEFAULT NULL COMMENT 'Protocol URL',
  `city_names` TEXT DEFAULT NULL COMMENT 'Open city names',
  `excluded_city_codes` TEXT DEFAULT NULL COMMENT 'Excluded city codes',
  `excluded_city_names` TEXT DEFAULT NULL COMMENT 'Excluded city names',
  `city_mode` TINYINT NOT NULL DEFAULT 0 COMMENT '0 all, 1 whitelist, 2 blacklist',
  `city_list` TEXT DEFAULT NULL COMMENT 'JSON city list',
  `working_hours` TEXT DEFAULT NULL COMMENT 'JSON working hour slots',
  `specified_channels` TEXT DEFAULT NULL COMMENT 'Specified channel codes',
  `excluded_channels` TEXT DEFAULT NULL COMMENT 'Excluded channel codes',
  `qualification_config` TEXT DEFAULT NULL COMMENT 'JSON qualification rules',
  `priority` INT NOT NULL DEFAULT 100 COMMENT 'Lower means higher priority',
  `weight` INT NOT NULL DEFAULT 100 COMMENT 'Display weight, higher means stronger priority',
  `daily_quota` INT DEFAULT NULL COMMENT 'Daily quota',
  `unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT 'Settlement unit price',
  `price_ratio` DECIMAL(10,2) DEFAULT NULL COMMENT 'Merchant price ratio',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT 'Remark',
  `ext_json` JSON DEFAULT NULL COMMENT 'Future compatibility extension',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_inst_id` (`inst_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Institution product and routing rules';

CREATE TABLE IF NOT EXISTS `institution_recharge_record` (
  `id` BIGINT PRIMARY KEY COMMENT 'Snowflake ID',
  `inst_id` BIGINT NOT NULL COMMENT 'Institution ID',
  `inst_code` VARCHAR(32) DEFAULT NULL COMMENT 'Institution code',
  `merchant_alias` VARCHAR(64) DEFAULT NULL COMMENT 'Merchant alias',
  `operator_name` VARCHAR(32) DEFAULT NULL COMMENT 'Operator name',
  `amount` DECIMAL(12,2) NOT NULL COMMENT 'Recharge amount',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT 'Remark',
  `recharge_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Recharge time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_inst_recharge_time` (`inst_id`, `recharge_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Institution recharge record';

CREATE TABLE IF NOT EXISTS `apply_order` (
  `id` BIGINT PRIMARY KEY COMMENT 'Snowflake ID',
  `order_no` VARCHAR(32) NOT NULL UNIQUE COMMENT 'Business order number',
  `channel_id` BIGINT NOT NULL COMMENT 'Channel ID',
  `channel_code` VARCHAR(32) DEFAULT NULL COMMENT 'Channel code snapshot',
  `inst_id` BIGINT DEFAULT NULL COMMENT 'Winning institution ID',
  `product_id` BIGINT DEFAULT NULL COMMENT 'Winning product ID',
  `push_id` BIGINT DEFAULT NULL COMMENT 'Winning push record ID',
  `trace_id` VARCHAR(64) NOT NULL COMMENT 'Trace ID',
  `phone_md5` CHAR(32) NOT NULL COMMENT 'Phone MD5',
  `phone_enc` VARCHAR(128) NOT NULL COMMENT 'Encrypted phone',
  `id_card_enc` VARCHAR(256) NOT NULL COMMENT 'Encrypted ID card',
  `user_name` VARCHAR(64) NOT NULL COMMENT 'Encrypted user name',
  `user_name_md5` CHAR(32) DEFAULT NULL COMMENT 'Name MD5 for exact search',
  `age` TINYINT NOT NULL COMMENT 'Age',
  `city_code` VARCHAR(8) NOT NULL COMMENT 'City code',
  `work_city` VARCHAR(64) DEFAULT NULL COMMENT 'Working city',
  `gender` TINYINT DEFAULT NULL COMMENT 'Gender',
  `profession` TINYINT DEFAULT NULL COMMENT 'Profession',
  `zhima` INT DEFAULT NULL COMMENT 'Zhima score',
  `house` TINYINT DEFAULT NULL COMMENT 'House status',
  `vehicle` TINYINT DEFAULT NULL COMMENT 'Vehicle status',
  `vehicle_status` VARCHAR(32) DEFAULT NULL COMMENT 'Vehicle running status',
  `vehicle_value` VARCHAR(64) DEFAULT NULL COMMENT 'Vehicle value',
  `provident_fund` TINYINT DEFAULT NULL COMMENT 'Provident fund status',
  `social_security` TINYINT DEFAULT NULL COMMENT 'Social security status',
  `commercial_insurance` TINYINT DEFAULT NULL COMMENT 'Commercial insurance status',
  `overdue` TINYINT DEFAULT NULL COMMENT 'Overdue status',
  `loan_amount` INT DEFAULT NULL COMMENT 'Requested loan amount',
  `loan_time` INT DEFAULT NULL COMMENT 'Loan term',
  `customer_level` VARCHAR(32) DEFAULT NULL COMMENT 'Customer level',
  `device_ip` VARCHAR(45) DEFAULT NULL COMMENT 'Device IP',
  `order_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 init, 1 pushing, 2 approved, 3 loaned, 9 failed',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT 'Failure reason',
  `settlement_price` DECIMAL(10,2) DEFAULT NULL COMMENT 'Winning settlement price',
  `follow_salesman` VARCHAR(64) DEFAULT NULL COMMENT 'Assigned salesman',
  `salesman_rating` TINYINT DEFAULT NULL COMMENT 'Salesman rating',
  `follow_remark` VARCHAR(255) DEFAULT NULL COMMENT 'Follow remark',
  `allocation_time` DATETIME DEFAULT NULL COMMENT 'Allocation time',
  `final_loan_amount` DECIMAL(10,2) DEFAULT NULL COMMENT 'Final loan amount',
  `ext_json` JSON DEFAULT NULL COMMENT 'Future compatibility extension',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_apply_phone` (`phone_md5`),
  INDEX `idx_apply_name` (`user_name_md5`),
  INDEX `idx_apply_channel_time` (`channel_id`, `created_at`),
  INDEX `idx_apply_status_time` (`order_status`, `created_at`),
  INDEX `idx_apply_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Application order';

CREATE TABLE IF NOT EXISTS `push_record` (
  `id` BIGINT PRIMARY KEY COMMENT 'Snowflake ID',
  `order_id` BIGINT DEFAULT NULL COMMENT 'Apply order ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT 'Business order number',
  `channel_id` BIGINT DEFAULT NULL COMMENT 'Channel ID snapshot',
  `inst_id` BIGINT NOT NULL COMMENT 'Institution ID',
  `inst_code` VARCHAR(32) DEFAULT NULL COMMENT 'Institution code snapshot',
  `product_id` BIGINT NOT NULL COMMENT 'Product ID',
  `trace_id` VARCHAR(64) NOT NULL COMMENT 'Trace ID',
  `request_id` VARCHAR(64) DEFAULT NULL COMMENT 'Pre-check or push request ID',
  `third_order_no` VARCHAR(64) DEFAULT NULL COMMENT 'Third-party order number',
  `push_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 pending, 1 pushing, 2 accepted, 3 approved, 4 rejected, 9 timeout',
  `request_log` TEXT DEFAULT NULL COMMENT 'Desensitized request payload',
  `response_log` TEXT DEFAULT NULL COMMENT 'Response payload',
  `error_msg` VARCHAR(512) DEFAULT NULL COMMENT 'Error message',
  `cost_ms` INT DEFAULT NULL COMMENT 'Elapsed time in ms',
  `pushed_at` DATETIME DEFAULT NULL COMMENT 'Push time',
  `notify_at` DATETIME DEFAULT NULL COMMENT 'Notify time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_push_order` (`order_id`),
  INDEX `idx_push_inst_time` (`inst_id`, `pushed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Push record';

CREATE TABLE IF NOT EXISTS `notify_record` (
  `id` BIGINT PRIMARY KEY COMMENT 'Snowflake ID',
  `inst_code` VARCHAR(32) NOT NULL COMMENT 'Institution code',
  `notify_no` VARCHAR(64) NOT NULL COMMENT 'Institution notify number',
  `order_no` VARCHAR(32) NOT NULL COMMENT 'Business order number',
  `status` VARCHAR(32) DEFAULT NULL COMMENT 'Notify status',
  `raw_body` TEXT DEFAULT NULL COMMENT 'Original body',
  `error_msg` VARCHAR(512) DEFAULT NULL COMMENT 'Notify error message',
  `is_processed` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether processed',
  `processed_at` DATETIME DEFAULT NULL COMMENT 'Processed time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_inst_notify` (`inst_code`, `notify_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Notify idempotency record';

CREATE TABLE IF NOT EXISTS `deduction_record` (
  `id` BIGINT PRIMARY KEY COMMENT 'Snowflake ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT 'Business order number',
  `channel_id` BIGINT NOT NULL COMMENT 'Channel ID',
  `inst_id` BIGINT DEFAULT NULL COMMENT 'Institution ID',
  `inst_code` VARCHAR(32) DEFAULT NULL COMMENT 'Institution code',
  `product_id` BIGINT DEFAULT NULL COMMENT 'Product ID',
  `deduct_type` TINYINT NOT NULL COMMENT '1 approve, 2 loan',
  `amount` DECIMAL(10,2) NOT NULL COMMENT 'Deduction amount',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 success, 0 cancelled',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT 'Remark',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_deduct_channel` (`channel_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Deduction record';

CREATE TABLE IF NOT EXISTS `report_hourly` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `stat_hour` CHAR(13) NOT NULL COMMENT 'Format yyyy-MM-dd HH',
  `channel_id` BIGINT NOT NULL COMMENT 'Channel ID',
  `inst_id` BIGINT DEFAULT NULL COMMENT 'Institution ID',
  `apply_cnt` INT NOT NULL DEFAULT 0 COMMENT 'Apply count',
  `push_cnt` INT NOT NULL DEFAULT 0 COMMENT 'Push count',
  `approve_cnt` INT NOT NULL DEFAULT 0 COMMENT 'Approve count',
  `loan_cnt` INT NOT NULL DEFAULT 0 COMMENT 'Loan count',
  `deduct_amount` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Deduction amount',
  UNIQUE KEY `uk_hour_channel_inst` (`stat_hour`, `channel_id`, `inst_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Hourly report snapshot';

CREATE TABLE IF NOT EXISTS `sys_admin` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(32) NOT NULL UNIQUE COMMENT 'Username',
  `password` VARCHAR(128) NOT NULL COMMENT 'BCrypt password',
  `real_name` VARCHAR(32) DEFAULT NULL COMMENT 'Real name',
  `role` VARCHAR(32) NOT NULL DEFAULT 'OPERATOR' COMMENT 'SUPER_ADMIN/ADMIN/OPERATOR',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Admin users';

INSERT IGNORE INTO `sys_admin` (`id`, `username`, `password`, `real_name`, `role`, `status`)
VALUES (1, 'admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36LNOQ/VKemXEJmzpN3vj52', 'Super Admin', 'SUPER_ADMIN', 1);

CREATE TABLE IF NOT EXISTS `sys_oper_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `admin_id` BIGINT DEFAULT NULL COMMENT 'Operator admin ID',
  `module` VARCHAR(32) NOT NULL COMMENT 'Module',
  `action` VARCHAR(64) NOT NULL COMMENT 'Action',
  `detail` TEXT DEFAULT NULL COMMENT 'Detail',
  `ip` VARCHAR(45) DEFAULT NULL COMMENT 'IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Operation log';

CREATE TABLE IF NOT EXISTS `city_config` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `province_name` VARCHAR(32) NOT NULL COMMENT 'Province name',
  `city_code` VARCHAR(8) NOT NULL COMMENT 'City code',
  `city_name` VARCHAR(64) NOT NULL COMMENT 'City name',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Display sort',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_city_code` (`city_code`),
  KEY `idx_province_status` (`province_name`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='City configuration';

-- Run doc/sql/city_config_seed.sql after init.sql to import the full 390-city seed set.
-- In production, execute the city seed with a UTF-8 client/session to preserve Chinese text.
