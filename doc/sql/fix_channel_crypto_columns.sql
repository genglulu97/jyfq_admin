ALTER TABLE `channel`
    ADD COLUMN `encrypt_type` VARCHAR(16) NOT NULL DEFAULT 'AES' COMMENT 'Encryption algorithm' AFTER `method_name`,
    ADD COLUMN `cipher_mode` VARCHAR(16) NOT NULL DEFAULT 'ECB' COMMENT 'Cipher mode' AFTER `encrypt_type`,
    ADD COLUMN `padding_mode` VARCHAR(32) NOT NULL DEFAULT 'PKCS5Padding' COMMENT 'Padding mode' AFTER `cipher_mode`,
    ADD COLUMN `iv_value` VARCHAR(64) DEFAULT NULL COMMENT 'IV value for CBC-like modes' AFTER `padding_mode`;
