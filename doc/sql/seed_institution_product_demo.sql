USE `loan_platform`;

INSERT INTO `channel` (`id`, `channel_code`, `channel_name`, `channel_type`, `app_key`, `fee_rate`, `status`, `remark`)
VALUES
  (20069, 'n_20069', '渠道前端测试', 'API', '1234567890abcdef', 88.00, 1, '机构产品联调渠道'),
  (30088, 'n_30088', '渠道扩展测试', 'API', '1234567890abcdef', 66.00, 1, '机构产品联调渠道')
ON DUPLICATE KEY UPDATE
  `channel_name` = VALUES(`channel_name`),
  `channel_type` = VALUES(`channel_type`),
  `app_key` = VALUES(`app_key`),
  `fee_rate` = VALUES(`fee_rate`),
  `status` = VALUES(`status`),
  `remark` = VALUES(`remark`);

UPDATE `institution`
SET `open_cities` = CASE `id`
                      WHEN 30001 THEN '山东省/青岛市'
                      WHEN 30002 THEN '湖南省/株洲市'
                      ELSE `open_cities`
                    END
WHERE `id` IN (30001, 30002);

UPDATE `institution_product`
SET `city_names` = CASE `id`
                     WHEN 40001 THEN '山东省/青岛市'
                     WHEN 40002 THEN '湖南省/株洲市'
                     ELSE `city_names`
                   END,
    `city_list` = CASE `id`
                    WHEN 40001 THEN '["山东省/青岛市","370200"]'
                    WHEN 40002 THEN '["湖南省/株洲市","430200"]'
                    ELSE `city_list`
                  END,
    `excluded_city_codes` = CASE `id`
                              WHEN 40001 THEN '["370100"]'
                              WHEN 40002 THEN '["430100"]'
                              ELSE `excluded_city_codes`
                            END,
    `excluded_city_names` = CASE `id`
                              WHEN 40001 THEN '山东省/济南市'
                              WHEN 40002 THEN '湖南省/长沙市'
                              ELSE `excluded_city_names`
                            END,
    `working_hours` = '[{"dayOfWeek":"MONDAY","startTime":"08:00","endTime":"18:00"},{"dayOfWeek":"TUESDAY","startTime":"08:00","endTime":"18:00"},{"dayOfWeek":"WEDNESDAY","startTime":"08:00","endTime":"18:00"},{"dayOfWeek":"THURSDAY","startTime":"08:00","endTime":"18:00"},{"dayOfWeek":"FRIDAY","startTime":"08:00","endTime":"18:00"}]',
    `specified_channels` = CASE `id`
                             WHEN 40001 THEN 'n_20069'
                             WHEN 40002 THEN 'n_20069,n_30088'
                             ELSE `specified_channels`
                           END,
    `excluded_channels` = CASE `id`
                            WHEN 40001 THEN 'n_30088'
                            WHEN 40002 THEN ''
                            ELSE `excluded_channels`
                          END,
    `qualification_config` = CASE `id`
                               WHEN 40001 THEN '{"house":1,"vehicle":1,"providentFund":1,"socialSecurity":1,"commercialInsurance":1,"profession":1,"overdue":1,"minZhima":600,"householdRegister":"山东"}'
                               WHEN 40002 THEN '{"house":0,"vehicle":1,"providentFund":0,"socialSecurity":1,"commercialInsurance":0,"profession":3,"overdue":1,"minZhima":650,"householdRegister":"湖南"}'
                               ELSE `qualification_config`
                             END,
    `priority` = CASE `id`
                   WHEN 40001 THEN 9900
                   WHEN 40002 THEN 9900
                   ELSE `priority`
                 END,
    `weight` = 100,
    `daily_quota` = 100,
    `unit_price` = 88.00,
    `price_ratio` = 1.00,
    `remark` = '-',
    `status` = 1
WHERE `id` IN (40001, 40002);
