USE `loan_platform`;

-- =====================================================
-- Data cleanup for historical dirty records
-- Goal:
-- 1. avoid NPE caused by null option fields
-- 2. clean orphan institution products
-- 3. optionally remove old local test data
-- =====================================================

-- -------------------------------------------------
-- 0. Preview suspicious data before cleanup
-- -------------------------------------------------
SELECT id, inst_id, product_name, city_names, excluded_city_names, created_at
FROM institution_product
WHERE city_names IS NULL
   OR excluded_city_names IS NULL
   OR inst_id IS NULL;

SELECT ip.id, ip.inst_id, ip.product_name, ip.created_at
FROM institution_product ip
LEFT JOIN institution i ON i.id = ip.inst_id
WHERE i.id IS NULL;

-- -------------------------------------------------
-- 1. Normalize option text fields to avoid null projections
-- -------------------------------------------------
UPDATE institution_product
SET city_names = ''
WHERE city_names IS NULL;

UPDATE institution_product
SET excluded_city_names = ''
WHERE excluded_city_names IS NULL;

UPDATE institution_product
SET specified_channels = ''
WHERE specified_channels IS NULL;

UPDATE institution_product
SET excluded_channels = ''
WHERE excluded_channels IS NULL;

UPDATE institution_product
SET city_list = '[]'
WHERE city_list IS NULL;

UPDATE institution_product
SET excluded_city_codes = '[]'
WHERE excluded_city_codes IS NULL;

-- -------------------------------------------------
-- 2. Remove orphan institution products
-- -------------------------------------------------
DELETE ip
FROM institution_product ip
LEFT JOIN institution i ON i.id = ip.inst_id
WHERE i.id IS NULL;

-- -------------------------------------------------
-- 3. Optional: remove obvious local test channels
-- Uncomment only when these records are no longer needed
-- -------------------------------------------------
-- DELETE FROM channel WHERE channel_code IN ('microsilver');

-- -------------------------------------------------
-- 4. Verification queries
-- -------------------------------------------------
SELECT COUNT(*) AS orphan_product_count
FROM institution_product ip
LEFT JOIN institution i ON i.id = ip.inst_id
WHERE i.id IS NULL;

SELECT COUNT(*) AS null_city_name_count
FROM institution_product
WHERE city_names IS NULL
   OR excluded_city_names IS NULL
   OR city_list IS NULL
   OR excluded_city_codes IS NULL;
