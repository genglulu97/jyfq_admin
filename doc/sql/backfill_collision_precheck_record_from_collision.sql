-- Backfill one summary pre-check detail per historical collision_record.
-- Historical product-level attempts cannot be reconstructed if they were not stored before.

USE `loan_platform`;

INSERT INTO `collision_precheck_record`
(`id`, `collision_id`, `collision_no`, `channel_id`, `channel_code`, `inst_id`, `inst_code`,
 `product_id`, `product_name_snapshot`, `trace_id`, `request_id`, `precheck_status`,
 `downstream_price`, `product_coefficient_price`, `upstream_channel_price`, `error_msg`,
 `prechecked_at`, `created_at`, `updated_at`)
SELECT
  c.`id`,
  c.`id`,
  c.`collision_no`,
  c.`channel_id`,
  c.`channel_code`,
  c.`inst_id`,
  c.`inst_code`,
  c.`product_id`,
  c.`product_name_snapshot`,
  c.`trace_id`,
  c.`trace_id`,
  CASE WHEN c.`collision_status` = 0 THEN 2 ELSE 4 END,
  c.`downstream_price`,
  c.`product_coefficient_price`,
  c.`upstream_channel_price`,
  c.`reject_reason`,
  c.`created_at`,
  c.`created_at`,
  c.`updated_at`
FROM `collision_record` c
WHERE NOT EXISTS (
  SELECT 1
  FROM `collision_precheck_record` d
  WHERE d.`collision_no` = c.`collision_no`
);
