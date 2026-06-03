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
    'channel',
    'min_price',
    "DECIMAL(18,2) DEFAULT NULL COMMENT 'Minimum response price for this channel' AFTER `fee_rate`"
);

CALL `sp_add_column_if_missing`(
    'channel',
    'max_price',
    "DECIMAL(18,2) DEFAULT NULL COMMENT 'Maximum response price for this channel' AFTER `min_price`"
);

CALL `sp_add_column_if_missing`(
    'channel',
    'price_return_mode',
    "VARCHAR(32) NOT NULL DEFAULT 'BEFORE_PROFIT' COMMENT 'BEFORE_PROFIT or AFTER_PROFIT' AFTER `max_price`"
);

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;
