-- Role and menu management tables.

CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `role_name` VARCHAR(32) NOT NULL COMMENT 'Role name',
  `role_code` VARCHAR(32) NOT NULL COMMENT 'Role code',
  `description` VARCHAR(255) DEFAULT NULL COMMENT 'Description',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Display sort',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_sys_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System roles';

CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Parent menu ID, 0 means root',
  `menu_name` VARCHAR(64) NOT NULL COMMENT 'Menu name',
  `menu_code` VARCHAR(64) NOT NULL COMMENT 'Menu code',
  `menu_type` TINYINT NOT NULL DEFAULT 2 COMMENT '1 catalog, 2 menu, 3 button',
  `path` VARCHAR(255) DEFAULT NULL COMMENT 'Route path',
  `component` VARCHAR(255) DEFAULT NULL COMMENT 'Frontend component path',
  `permission` VARCHAR(128) DEFAULT NULL COMMENT 'Permission code',
  `icon` VARCHAR(64) DEFAULT NULL COMMENT 'Menu icon',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Display sort',
  `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '1 visible, 0 hidden',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT 'Remark',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_sys_menu_code` (`menu_code`),
  KEY `idx_sys_menu_parent_sort` (`parent_id`, `sort`),
  KEY `idx_sys_menu_permission` (`permission`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System menus and permissions';

CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL COMMENT 'Role ID',
  `menu_id` BIGINT NOT NULL COMMENT 'Menu ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_sys_role_menu` (`role_id`, `menu_id`),
  KEY `idx_sys_role_menu_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role menu relations';

INSERT IGNORE INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `status`, `sort`)
VALUES
  (1, '超级管理员', 'SUPER_ADMIN', '系统内置超级管理员', 1, 1),
  (2, '管理员', 'ADMIN', '系统内置管理员', 1, 2),
  (3, '操作员', 'OPERATOR', '系统内置操作员', 1, 3);

INSERT IGNORE INTO `sys_menu`
(`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `visible`, `status`)
VALUES
  (100, 0, '系统管理', 'SYSTEM', 1, '/system', NULL, NULL, 'settings', 100, 1, 1),
  (110, 100, '用户管理', 'SYSTEM_USER', 2, '/system/user', 'system/user/index', 'system:user:list', 'users', 110, 1, 1),
  (111, 110, '新增用户', 'SYSTEM_USER_ADD', 3, NULL, NULL, 'system:user:add', NULL, 111, 0, 1),
  (112, 110, '修改用户', 'SYSTEM_USER_UPDATE', 3, NULL, NULL, 'system:user:update', NULL, 112, 0, 1),
  (113, 110, '删除用户', 'SYSTEM_USER_DELETE', 3, NULL, NULL, 'system:user:delete', NULL, 113, 0, 1),
  (114, 110, '启停用户', 'SYSTEM_USER_TOGGLE', 3, NULL, NULL, 'system:user:toggle', NULL, 114, 0, 1),
  (115, 110, '重置密码', 'SYSTEM_USER_RESET_PASSWORD', 3, NULL, NULL, 'system:user:reset-password', NULL, 115, 0, 1),
  (120, 100, '角色权限', 'SYSTEM_ROLE', 2, '/system/role', 'system/role/index', 'system:role:list', 'shield', 120, 1, 1),
  (121, 120, '新增角色', 'SYSTEM_ROLE_ADD', 3, NULL, NULL, 'system:role:add', NULL, 121, 0, 1),
  (122, 120, '修改角色', 'SYSTEM_ROLE_UPDATE', 3, NULL, NULL, 'system:role:update', NULL, 122, 0, 1),
  (123, 120, '删除角色', 'SYSTEM_ROLE_DELETE', 3, NULL, NULL, 'system:role:delete', NULL, 123, 0, 1),
  (124, 120, '启停角色', 'SYSTEM_ROLE_TOGGLE', 3, NULL, NULL, 'system:role:toggle', NULL, 124, 0, 1),
  (125, 120, '分配菜单', 'SYSTEM_ROLE_ASSIGN_MENU', 3, NULL, NULL, 'system:role:assign-menu', NULL, 125, 0, 1),
  (130, 100, '菜单管理', 'SYSTEM_MENU', 2, '/system/menu', 'system/menu/index', 'system:menu:list', 'list-checks', 130, 1, 1),
  (131, 130, '新增菜单', 'SYSTEM_MENU_ADD', 3, NULL, NULL, 'system:menu:add', NULL, 131, 0, 1),
  (132, 130, '修改菜单', 'SYSTEM_MENU_UPDATE', 3, NULL, NULL, 'system:menu:update', NULL, 132, 0, 1),
  (133, 130, '删除菜单', 'SYSTEM_MENU_DELETE', 3, NULL, NULL, 'system:menu:delete', NULL, 133, 0, 1),
  (134, 130, '启停菜单', 'SYSTEM_MENU_TOGGLE', 3, NULL, NULL, 'system:menu:toggle', NULL, 134, 0, 1),
  (140, 100, '参数配置', 'SYSTEM_CONFIG', 2, '/system/config', 'system/config/index', 'system:config:list', 'sliders-horizontal', 140, 1, 1);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu`;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, `id` FROM `sys_menu`;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES
  (3, 100),
  (3, 110);
