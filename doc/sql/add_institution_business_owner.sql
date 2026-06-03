ALTER TABLE `institution`
  ADD COLUMN `business_owner` VARCHAR(64) DEFAULT NULL COMMENT 'Business owner' AFTER `merchant_type`;
