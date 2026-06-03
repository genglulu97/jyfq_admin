-- Clear non-authoritative customer_level values until downstream star-level sync is implemented.
-- customer_level should only be written by downstream institution return/query logic.

USE `loan_platform`;

UPDATE `apply_order`
SET `customer_level` = NULL
WHERE `customer_level` IS NOT NULL;

UPDATE `collision_record`
SET `customer_level` = NULL
WHERE `customer_level` IS NOT NULL;

UPDATE `institution_customer`
SET `customer_level` = NULL
WHERE `customer_level` IS NOT NULL;
