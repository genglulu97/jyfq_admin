-- Fix missing columns for institution API config page
-- Execute these statements one by one in database loan_platform.
-- If a statement reports "Duplicate column name", skip it and continue.

USE `loan_platform`;

ALTER TABLE `institution`
    ADD COLUMN `business_code` VARCHAR(64) DEFAULT NULL COMMENT 'Partner business code' AFTER `merchant_type`;

ALTER TABLE `institution`
    ADD COLUMN `pre_check_url` VARCHAR(256) DEFAULT NULL COMMENT 'Pre-check API URL' AFTER `business_code`;

ALTER TABLE `institution`
    ADD COLUMN `api_push_url` VARCHAR(256) DEFAULT NULL COMMENT 'Push API URL' AFTER `pre_check_url`;

ALTER TABLE `institution`
    ADD COLUMN `api_notify_url` VARCHAR(256) DEFAULT NULL COMMENT 'Notify callback URL' AFTER `api_push_url`;

ALTER TABLE `institution`
    ADD COLUMN `app_key` VARCHAR(128) DEFAULT NULL COMMENT 'Shared secret' AFTER `api_notify_url`;

ALTER TABLE `institution`
    ADD COLUMN `rsa_public_key` TEXT DEFAULT NULL COMMENT 'RSA public key' AFTER `app_key`;

ALTER TABLE `institution`
    ADD COLUMN `encrypt_type` VARCHAR(16) NOT NULL DEFAULT 'PLAIN' COMMENT 'AES/RSA/PLAIN' AFTER `rsa_public_key`;

ALTER TABLE `institution`
    ADD COLUMN `notify_encrypt_type` VARCHAR(16) NOT NULL DEFAULT 'PLAIN' COMMENT 'Notify encryption mode' AFTER `encrypt_type`;

ALTER TABLE `institution`
    ADD COLUMN `timeout_ms` INT NOT NULL DEFAULT 3000 COMMENT 'Adapter timeout in ms' AFTER `notify_encrypt_type`;

ALTER TABLE `institution`
    ADD COLUMN `api_method_name` VARCHAR(64) DEFAULT NULL COMMENT 'API method name' AFTER `api_merchant`;

ALTER TABLE `institution`
    ADD COLUMN `specified_channel` VARCHAR(64) DEFAULT NULL COMMENT 'Specified channel' AFTER `api_method_name`;

ALTER TABLE `institution`
    ADD COLUMN `excluded_channels` VARCHAR(255) DEFAULT NULL COMMENT 'Excluded channels' AFTER `specified_channel`;

ALTER TABLE `institution`
    ADD COLUMN `account_balance` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Merchant balance' AFTER `excluded_channels`;

ALTER TABLE `institution`
    ADD COLUMN `recharge_total` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT 'Recharge total' AFTER `account_balance`;
