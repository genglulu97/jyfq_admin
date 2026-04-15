USE `loan_platform`;

DELETE FROM `push_record`
WHERE `order_no` IN (
  SELECT `order_no` FROM (
    SELECT `order_no` FROM `apply_order` WHERE `channel_code` = 'microsilver'
  ) t
);

DELETE FROM `deduction_record`
WHERE `order_no` IN (
  SELECT `order_no` FROM (
    SELECT `order_no` FROM `apply_order` WHERE `channel_code` = 'microsilver'
  ) t
);

DELETE FROM `notify_record`
WHERE `order_no` IN (
  SELECT `order_no` FROM (
    SELECT `order_no` FROM `apply_order` WHERE `channel_code` = 'microsilver'
  ) t
);

DELETE FROM `apply_order`
WHERE `channel_code` = 'microsilver';
