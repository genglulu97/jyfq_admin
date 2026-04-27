ALTER TABLE `institution`
    ADD COLUMN `cipher_mode` VARCHAR(16) NOT NULL DEFAULT 'CBC' COMMENT 'Request cipher mode' AFTER `encrypt_type`,
    ADD COLUMN `padding_mode` VARCHAR(32) NOT NULL DEFAULT 'PKCS5Padding' COMMENT 'Request padding mode' AFTER `cipher_mode`,
    ADD COLUMN `iv_value` VARCHAR(64) DEFAULT NULL COMMENT 'Request IV value' AFTER `padding_mode`,
    ADD COLUMN `notify_cipher_mode` VARCHAR(16) NOT NULL DEFAULT 'CBC' COMMENT 'Notify cipher mode' AFTER `notify_encrypt_type`,
    ADD COLUMN `notify_padding_mode` VARCHAR(32) NOT NULL DEFAULT 'PKCS5Padding' COMMENT 'Notify padding mode' AFTER `notify_cipher_mode`,
    ADD COLUMN `notify_iv_value` VARCHAR(64) DEFAULT NULL COMMENT 'Notify IV value' AFTER `notify_padding_mode`;
