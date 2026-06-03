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
    'channel_type',
    "VARCHAR(64) NOT NULL DEFAULT '全流程API-CPS' COMMENT 'Channel type for matching' AFTER `merchant_type`"
);

UPDATE `institution`
SET `channel_type` = '全流程API-CPS'
WHERE `channel_type` IS NULL OR `channel_type` = '';

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;
