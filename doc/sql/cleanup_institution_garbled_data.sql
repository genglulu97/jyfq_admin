USE `loan_platform`;

DELETE FROM `institution_recharge_record`
WHERE `inst_id` IN (963, 964);

DELETE FROM `institution_product`
WHERE `inst_id` IN (963, 964);

DELETE FROM `institution`
WHERE `id` IN (963, 964);
