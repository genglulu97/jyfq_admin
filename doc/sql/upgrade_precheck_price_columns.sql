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

CALL `sp_add_column_if_missing`('push_record', 'downstream_price', "DECIMAL(18,2) DEFAULT NULL COMMENT 'Downstream returned price' AFTER `response_log`");
CALL `sp_add_column_if_missing`('push_record', 'product_coefficient_price', "DECIMAL(18,2) DEFAULT NULL COMMENT 'Downstream price multiplied by product price ratio' AFTER `downstream_price`");
CALL `sp_add_column_if_missing`('push_record', 'upstream_channel_price', "DECIMAL(18,2) DEFAULT NULL COMMENT 'Product coefficient price multiplied by channel fee rate' AFTER `product_coefficient_price`");

CALL `sp_add_column_if_missing`('collision_record', 'downstream_price', "DECIMAL(18,2) DEFAULT NULL COMMENT 'Downstream returned price' AFTER `settlement_price`");
CALL `sp_add_column_if_missing`('collision_record', 'product_coefficient_price', "DECIMAL(18,2) DEFAULT NULL COMMENT 'Downstream price multiplied by product price ratio' AFTER `downstream_price`");
CALL `sp_add_column_if_missing`('collision_record', 'upstream_channel_price', "DECIMAL(18,2) DEFAULT NULL COMMENT 'Product coefficient price multiplied by channel fee rate' AFTER `product_coefficient_price`");

ALTER TABLE `push_record`
  MODIFY COLUMN `downstream_price` DECIMAL(18,2) DEFAULT NULL COMMENT 'Downstream returned price',
  MODIFY COLUMN `product_coefficient_price` DECIMAL(18,2) DEFAULT NULL COMMENT 'Downstream price multiplied by product price ratio',
  MODIFY COLUMN `upstream_channel_price` DECIMAL(18,2) DEFAULT NULL COMMENT 'Product coefficient price multiplied by channel fee rate';

ALTER TABLE `collision_record`
  MODIFY COLUMN `downstream_price` DECIMAL(18,2) DEFAULT NULL COMMENT 'Downstream returned price',
  MODIFY COLUMN `product_coefficient_price` DECIMAL(18,2) DEFAULT NULL COMMENT 'Downstream price multiplied by product price ratio',
  MODIFY COLUMN `upstream_channel_price` DECIMAL(18,2) DEFAULT NULL COMMENT 'Product coefficient price multiplied by channel fee rate';

UPDATE `push_record` p
LEFT JOIN `institution_product` ip ON ip.id = p.product_id
LEFT JOIN `channel` ch ON ch.id = p.channel_id
SET p.downstream_price = ROUND(CAST(JSON_UNQUOTE(JSON_EXTRACT(p.response_log, '$.price')) AS DECIMAL(18,5)), 2),
    p.product_coefficient_price = ROUND(CAST(JSON_UNQUOTE(JSON_EXTRACT(p.response_log, '$.price')) AS DECIMAL(18,5)) * COALESCE(ip.price_ratio, 1), 2),
    p.upstream_channel_price = ROUND(CAST(JSON_UNQUOTE(JSON_EXTRACT(p.response_log, '$.price')) AS DECIMAL(18,5)) * COALESCE(ip.price_ratio, 1) * COALESCE(ch.fee_rate, 0), 2)
WHERE p.response_log IS NOT NULL
  AND JSON_EXTRACT(p.response_log, '$.price') IS NOT NULL;

UPDATE `collision_record` c
JOIN `push_record` p
  ON p.order_id IS NULL
 AND p.order_no = c.collision_no
 AND p.product_id = c.product_id
SET c.downstream_price = p.downstream_price,
    c.product_coefficient_price = p.product_coefficient_price,
    c.upstream_channel_price = p.upstream_channel_price,
    c.settlement_price = p.product_coefficient_price
WHERE p.downstream_price IS NOT NULL;

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;
