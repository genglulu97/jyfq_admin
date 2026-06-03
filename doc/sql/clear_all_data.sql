-- =====================================================
-- Clear all data in loan_platform
-- Database: loan_platform
-- Note:
-- 1. This script keeps database/table structures and removes table data.
-- 2. AUTO_INCREMENT values are reset.
-- 3. TRUNCATE is not rollback-friendly; back up the database before running it.
-- =====================================================

USE `loan_platform`;

SET @OLD_FOREIGN_KEY_CHECKS := @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `apply_order`;
TRUNCATE TABLE `channel`;
TRUNCATE TABLE `city_config`;
TRUNCATE TABLE `collision_precheck_record`;
TRUNCATE TABLE `collision_record`;
TRUNCATE TABLE `deduction_record`;
TRUNCATE TABLE `institution`;
TRUNCATE TABLE `institution_customer`;
TRUNCATE TABLE `institution_product`;
TRUNCATE TABLE `institution_recharge_record`;
TRUNCATE TABLE `notify_record`;
TRUNCATE TABLE `push_record`;
TRUNCATE TABLE `report_hourly`;
TRUNCATE TABLE `sys_admin`;
TRUNCATE TABLE `sys_menu`;
TRUNCATE TABLE `sys_oper_log`;
TRUNCATE TABLE `sys_role`;
TRUNCATE TABLE `sys_role_menu`;

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;

SELECT
  `TABLE_NAME` AS table_name,
  `TABLE_ROWS` AS estimated_rows
FROM `INFORMATION_SCHEMA`.`TABLES`
WHERE `TABLE_SCHEMA` = DATABASE()
  AND `TABLE_TYPE` = 'BASE TABLE'
ORDER BY `TABLE_NAME`;
