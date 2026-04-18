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

-- 1) Add the snapshot column if it is still missing.
CALL `sp_add_column_if_missing`(
    'apply_order',
    'product_name_snapshot',
    "VARCHAR(128) DEFAULT NULL COMMENT 'Winning product name snapshot'"
);

-- 2) Backfill historical rows.
UPDATE `apply_order` ao
LEFT JOIN `institution_product` ip ON ao.`product_id` = ip.`id`
LEFT JOIN `institution` i ON ao.`inst_id` = i.`id`
SET ao.`product_name_snapshot` = COALESCE(ip.`product_name`, i.`merchant_alias`, i.`inst_name`)
WHERE (ao.`product_name_snapshot` IS NULL OR ao.`product_name_snapshot` = '')
  AND COALESCE(ip.`product_name`, i.`merchant_alias`, i.`inst_name`) IS NOT NULL;

-- 3) Verify the backfill result.
SELECT
  COUNT(*) AS `total_orders`,
  SUM(CASE WHEN `product_name_snapshot` IS NOT NULL AND `product_name_snapshot` <> '' THEN 1 ELSE 0 END) AS `filled_orders`,
  SUM(CASE WHEN `product_name_snapshot` IS NULL OR `product_name_snapshot` = '' THEN 1 ELSE 0 END) AS `empty_orders`
FROM `apply_order`;

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;
