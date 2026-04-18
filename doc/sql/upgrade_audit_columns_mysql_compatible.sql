-- =====================================================
-- Compatible audit-column upgrade script for MySQL 5.7+
-- Purpose:
--   add created_at / create_by / updated_at / update_by
--   to all core tables without using
--   "ADD COLUMN IF NOT EXISTS"
-- =====================================================

USE `loan_platform`;

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;
DELIMITER $$

CREATE PROCEDURE `sp_add_column_if_missing`(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', p_table_name, '` ADD COLUMN `',
            p_column_name, '` ', p_column_definition
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL `sp_add_column_if_missing`('channel', 'created_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('channel', 'create_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Created by'");
CALL `sp_add_column_if_missing`('channel', 'updated_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('channel', 'update_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Updated by'");

CALL `sp_add_column_if_missing`('institution', 'created_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('institution', 'create_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Created by'");
CALL `sp_add_column_if_missing`('institution', 'updated_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('institution', 'update_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Updated by'");

CALL `sp_add_column_if_missing`('institution_product', 'created_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('institution_product', 'create_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Created by'");
CALL `sp_add_column_if_missing`('institution_product', 'updated_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('institution_product', 'update_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Updated by'");

CALL `sp_add_column_if_missing`('institution_recharge_record', 'created_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('institution_recharge_record', 'create_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Created by'");
CALL `sp_add_column_if_missing`('institution_recharge_record', 'updated_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('institution_recharge_record', 'update_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Updated by'");

CALL `sp_add_column_if_missing`('apply_order', 'created_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('apply_order', 'create_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Created by'");
CALL `sp_add_column_if_missing`('apply_order', 'updated_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('apply_order', 'update_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Updated by'");
CALL `sp_add_column_if_missing`('apply_order', 'product_name_snapshot', "VARCHAR(128) DEFAULT NULL COMMENT 'Winning product name snapshot'");

CALL `sp_add_column_if_missing`('push_record', 'created_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('push_record', 'create_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Created by'");
CALL `sp_add_column_if_missing`('push_record', 'updated_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('push_record', 'update_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Updated by'");

CALL `sp_add_column_if_missing`('notify_record', 'created_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('notify_record', 'create_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Created by'");
CALL `sp_add_column_if_missing`('notify_record', 'updated_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('notify_record', 'update_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Updated by'");

CALL `sp_add_column_if_missing`('deduction_record', 'created_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('deduction_record', 'create_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Created by'");
CALL `sp_add_column_if_missing`('deduction_record', 'updated_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('deduction_record', 'update_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Updated by'");

CALL `sp_add_column_if_missing`('report_hourly', 'created_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('report_hourly', 'create_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Created by'");
CALL `sp_add_column_if_missing`('report_hourly', 'updated_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('report_hourly', 'update_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Updated by'");

CALL `sp_add_column_if_missing`('sys_admin', 'created_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('sys_admin', 'create_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Created by'");
CALL `sp_add_column_if_missing`('sys_admin', 'updated_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('sys_admin', 'update_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Updated by'");

CALL `sp_add_column_if_missing`('sys_oper_log', 'created_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('sys_oper_log', 'create_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Created by'");
CALL `sp_add_column_if_missing`('sys_oper_log', 'updated_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('sys_oper_log', 'update_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Updated by'");

CALL `sp_add_column_if_missing`('city_config', 'created_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('city_config', 'create_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Created by'");
CALL `sp_add_column_if_missing`('city_config', 'updated_at', "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL `sp_add_column_if_missing`('city_config', 'update_by', "VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT 'Updated by'");

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;
