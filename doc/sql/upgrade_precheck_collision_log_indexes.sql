-- =====================================================
-- Pre-check collision log query indexes
-- Purpose:
--   Keep the admin collision-log page on paged/indexed reads while
--   preCheck collision records grow in collision_record.
-- =====================================================

USE `loan_platform`;

CREATE TABLE IF NOT EXISTS `collision_precheck_record` (
  `id` BIGINT PRIMARY KEY COMMENT 'Snowflake ID',
  `collision_id` BIGINT DEFAULT NULL COMMENT 'Collision record ID',
  `collision_no` VARCHAR(32) NOT NULL COMMENT 'Collision record number',
  `channel_id` BIGINT DEFAULT NULL COMMENT 'Channel ID snapshot',
  `channel_code` VARCHAR(32) DEFAULT NULL COMMENT 'Channel code snapshot',
  `inst_id` BIGINT DEFAULT NULL COMMENT 'Institution ID',
  `inst_code` VARCHAR(32) DEFAULT NULL COMMENT 'Institution code snapshot',
  `product_id` BIGINT DEFAULT NULL COMMENT 'Product ID',
  `product_name_snapshot` VARCHAR(128) DEFAULT NULL COMMENT 'Product name snapshot',
  `trace_id` VARCHAR(64) DEFAULT NULL COMMENT 'Trace ID',
  `request_id` VARCHAR(64) DEFAULT NULL COMMENT 'Downstream pre-check request ID',
  `third_order_no` VARCHAR(64) DEFAULT NULL COMMENT 'Downstream pre-check order number',
  `precheck_status` TINYINT NOT NULL DEFAULT 0 COMMENT '2 passed, 4 rejected, 9 abnormal or timeout',
  `request_log` TEXT DEFAULT NULL COMMENT 'Desensitized request payload',
  `response_log` TEXT DEFAULT NULL COMMENT 'Response payload',
  `downstream_price` DECIMAL(18,2) DEFAULT NULL COMMENT 'Downstream returned price',
  `product_coefficient_price` DECIMAL(18,2) DEFAULT NULL COMMENT 'Downstream price multiplied by product price ratio',
  `upstream_channel_price` DECIMAL(18,2) DEFAULT NULL COMMENT 'Product coefficient price multiplied by channel fee rate',
  `error_msg` VARCHAR(512) DEFAULT NULL COMMENT 'Error message',
  `cost_ms` INT DEFAULT NULL COMMENT 'Elapsed time in ms',
  `prechecked_at` DATETIME DEFAULT NULL COMMENT 'Pre-check time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Collision pre-check product detail';

DROP PROCEDURE IF EXISTS `sp_add_index_if_missing`;
DELIMITER $$

CREATE PROCEDURE `sp_add_index_if_missing`(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', p_table_name, '` ADD INDEX `',
            p_index_name, '` ', p_index_definition
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL `sp_add_index_if_missing`(
    'collision_record',
    'idx_collision_name',
    '(`user_name_md5`, `created_at`)'
);

CALL `sp_add_index_if_missing`(
    'collision_record',
    'idx_collision_channel_code_time',
    '(`channel_code`, `created_at`)'
);

CALL `sp_add_index_if_missing`(
    'collision_precheck_record',
    'idx_collision_precheck_no',
    '(`collision_no`, `prechecked_at`)'
);

CALL `sp_add_index_if_missing`(
    'collision_precheck_record',
    'idx_collision_precheck_time',
    '(`prechecked_at`)'
);

CALL `sp_add_index_if_missing`(
    'collision_precheck_record',
    'idx_collision_precheck_status_time',
    '(`precheck_status`, `prechecked_at`)'
);

CALL `sp_add_index_if_missing`(
    'collision_precheck_record',
    'idx_collision_precheck_channel_time',
    '(`channel_code`, `prechecked_at`)'
);

CALL `sp_add_index_if_missing`(
    'collision_precheck_record',
    'idx_collision_precheck_product',
    '(`product_id`, `prechecked_at`)'
);

INSERT INTO `collision_precheck_record`
(`id`, `collision_id`, `collision_no`, `channel_id`, `channel_code`, `inst_id`, `inst_code`,
 `product_id`, `product_name_snapshot`, `trace_id`, `request_id`, `precheck_status`,
 `downstream_price`, `product_coefficient_price`, `upstream_channel_price`, `error_msg`,
 `prechecked_at`, `created_at`, `updated_at`)
SELECT
  c.`id`,
  c.`id`,
  c.`collision_no`,
  c.`channel_id`,
  c.`channel_code`,
  c.`inst_id`,
  c.`inst_code`,
  c.`product_id`,
  c.`product_name_snapshot`,
  c.`trace_id`,
  c.`trace_id`,
  CASE WHEN c.`collision_status` = 0 THEN 2 ELSE 4 END,
  c.`downstream_price`,
  c.`product_coefficient_price`,
  c.`upstream_channel_price`,
  c.`reject_reason`,
  c.`created_at`,
  c.`created_at`,
  c.`updated_at`
FROM `collision_record` c
WHERE NOT EXISTS (
  SELECT 1
  FROM `collision_precheck_record` d
  WHERE d.`collision_no` = c.`collision_no`
);

DROP PROCEDURE IF EXISTS `sp_add_index_if_missing`;
