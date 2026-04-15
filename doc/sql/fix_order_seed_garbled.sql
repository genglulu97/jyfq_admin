USE `loan_platform`;

UPDATE `channel`
SET `channel_name` = CONVERT(0xe6b8a0e98193e5898de7abafe6b58be8af95 USING utf8mb4),
    `remark` = CONVERT(0xe5898de7abafe88194e8b083e6a0b7e4be8b USING utf8mb4)
WHERE `id` = 20069;

UPDATE `institution`
SET `inst_name` = CASE `id`
                    WHEN 30001 THEN CONVERT(0xe5bfabe69893e8b4b74232 USING utf8mb4)
                    WHEN 30002 THEN CONVERT(0xe5bfabe69893e8b4b74231 USING utf8mb4)
                    ELSE `inst_name`
                  END,
    `merchant_alias` = CASE `id`
                         WHEN 30001 THEN CONVERT(0xe5bfabe69893e8b4b74232 USING utf8mb4)
                         WHEN 30002 THEN CONVERT(0xe5bfabe69893e8b4b74231 USING utf8mb4)
                         ELSE `merchant_alias`
                       END,
    `merchant_type` = CONVERT(0xe69cbae69e84 USING utf8mb4),
    `remark` = CONVERT(0xe5898de7abafe88194e8b083e6a0b7e4be8b USING utf8mb4)
WHERE `id` IN (30001, 30002);

UPDATE `institution_product`
SET `product_name` = CASE `id`
                       WHEN 40001 THEN CONVERT(0xe5bfabe69893e8b4b74232 USING utf8mb4)
                       WHEN 40002 THEN CONVERT(0xe5bfabe69893e8b4b74231 USING utf8mb4)
                       ELSE `product_name`
                     END
WHERE `id` IN (40001, 40002);

UPDATE `apply_order`
SET `work_city` = CASE `order_no`
                    WHEN 'TEST20260414170734' THEN CONVERT(0xe4b88ae6b5b7e5b8822fe4b88ae6b5b7e5b882 USING utf8mb4)
                    WHEN 'TEST20260414170700' THEN CONVERT(0xe58c97e4baace5b8822fe58c97e4baace5b882 USING utf8mb4)
                    ELSE `work_city`
                  END,
    `vehicle_status` = CASE `order_no`
                         WHEN 'TEST20260414170734' THEN CONVERT(0xe69caae79fa5 USING utf8mb4)
                         WHEN 'TEST20260414170700' THEN CONVERT(0xe696b0e8bda6 USING utf8mb4)
                         ELSE `vehicle_status`
                       END,
    `vehicle_value` = CASE `order_no`
                        WHEN 'TEST20260414170734' THEN CONVERT(0xe69caae79fa5 USING utf8mb4)
                        WHEN 'TEST20260414170700' THEN CONVERT(0x3135e4b8872b USING utf8mb4)
                        ELSE `vehicle_value`
                      END,
    `customer_level` = CASE `order_no`
                         WHEN 'TEST20260414170734' THEN CONVERT(0x34e6989f USING utf8mb4)
                         WHEN 'TEST20260414170700' THEN CONVERT(0x33e6989f USING utf8mb4)
                         ELSE `customer_level`
                       END,
    `follow_remark` = CASE `order_no`
                        WHEN 'TEST20260414170734' THEN CONVERT(0xe5aea2e688b7e8b584e8b4a8e8be83e5a5bdefbc8ce5b7b2e5ae8ce68890e88194e8b083e58886e9858d USING utf8mb4)
                        WHEN 'TEST20260414170700' THEN CONVERT(0xe7ad89e5be85e4b88be6b8b8e69c80e7bb88e694bee6acbee59b9ee4bca0 USING utf8mb4)
                        ELSE `follow_remark`
                      END
WHERE `order_no` IN ('TEST20260414170734', 'TEST20260414170700');
