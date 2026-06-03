ALTER TABLE crm_customer
  ADD COLUMN crm_inst_id BIGINT NULL AFTER mobile_md5,
  ADD COLUMN crm_inst_code VARCHAR(64) NULL AFTER crm_inst_id,
  ADD COLUMN crm_inst_name VARCHAR(128) NULL AFTER crm_inst_code,
  ADD COLUMN source_inst_id BIGINT NULL AFTER crm_inst_name,
  ADD COLUMN source_inst_code VARCHAR(64) NULL AFTER source_inst_id,
  ADD COLUMN source_inst_name VARCHAR(128) NULL AFTER source_inst_code,
  ADD COLUMN product_id BIGINT NULL AFTER source_inst_name,
  ADD COLUMN product_name VARCHAR(128) NULL AFTER product_id,
  ADD COLUMN source_order_no VARCHAR(64) NULL AFTER product_name,
  ADD COLUMN source_collision_no VARCHAR(64) NULL AFTER source_order_no,
  ADD KEY idx_crm_inst_mobile_md5 (crm_inst_id, mobile_md5),
  ADD KEY idx_crm_inst_id (crm_inst_id),
  ADD KEY idx_source_inst_id (source_inst_id),
  ADD KEY idx_product_id (product_id),
  ADD KEY idx_source_order_no (source_order_no),
  ADD KEY idx_source_collision_no (source_collision_no);

ALTER TABLE crm_institution_config
  ADD COLUMN platform_inst_id BIGINT NULL AFTER inst_name,
  ADD COLUMN platform_inst_code VARCHAR(64) NULL AFTER platform_inst_id,
  ADD COLUMN platform_inst_name VARCHAR(128) NULL AFTER platform_inst_code,
  ADD COLUMN crm_inst_id BIGINT NULL AFTER platform_inst_name,
  ADD COLUMN crm_inst_code VARCHAR(64) NULL AFTER crm_inst_id,
  ADD COLUMN crm_inst_name VARCHAR(128) NULL AFTER crm_inst_code,
  ADD COLUMN auto_push TINYINT NOT NULL DEFAULT 1 AFTER crm_inst_name,
  ADD KEY idx_platform_inst_id (platform_inst_id),
  ADD KEY idx_crm_inst_id (crm_inst_id);

UPDATE crm_institution_config
SET crm_inst_id = inst_id,
    crm_inst_code = inst_code,
    crm_inst_name = inst_name
WHERE crm_inst_id IS NULL;
