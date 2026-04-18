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

CALL `sp_add_column_if_missing`(
    'institution',
    'business_code',
    "VARCHAR(64) DEFAULT NULL COMMENT 'Partner business code'"
);

CALL `sp_add_column_if_missing`(
    'institution',
    'pre_check_url',
    "VARCHAR(256) DEFAULT NULL COMMENT 'Pre-check API URL'"
);

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;
