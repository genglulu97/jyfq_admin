-- Reserve customer_level for downstream institution returned star level.
-- This script only fixes column comments. It does not derive values from upstream data.

USE `loan_platform`;

ALTER TABLE `apply_order`
  MODIFY COLUMN `customer_level` VARCHAR(32) DEFAULT NULL COMMENT 'Downstream returned customer star level';

ALTER TABLE `collision_record`
  MODIFY COLUMN `customer_level` VARCHAR(32) DEFAULT NULL COMMENT 'Downstream returned customer star level';

ALTER TABLE `institution_customer`
  MODIFY COLUMN `customer_level` VARCHAR(32) DEFAULT NULL COMMENT 'Downstream returned customer star level';
