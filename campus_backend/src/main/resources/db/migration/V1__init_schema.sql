-- V1__init_schema.sql
-- 全新环境全量建库脚本

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 角色表
CREATE TABLE `role` (
                        `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                        `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码，如 admin/student',
                        `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
                        `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 2. 权限表
CREATE TABLE `permission` (
                              `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                              `code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限编码，如 activity:edit',
                              `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
                              `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_permission_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 3. 角色权限关联表
CREATE TABLE `role_permission` (
                                   `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                   `role_id` bigint unsigned NOT NULL,
                                   `permission_id` bigint unsigned NOT NULL,
                                   `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
                                   KEY `idx_rp_permission` (`permission_id`),
                                   CONSTRAINT `fk_rp_permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`),
                                   CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 4. 用户表
CREATE TABLE `user` (
                        `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                        `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录名',
                        `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码哈希（不存明文）',
                        `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                        `role_id` bigint unsigned NOT NULL DEFAULT '4',
                        `club_id` bigint unsigned DEFAULT NULL COMMENT '社团ID，CLUB_ADMIN必填',
                        `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
                        `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
                        `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
                        `status` tinyint NOT NULL DEFAULT '1' COMMENT '1=正常 0=禁用',
                        `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
                        `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_user_username` (`username`),
                        KEY `idx_user_role` (`role_id`),
                        CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 5. 社团表
CREATE TABLE `club` (
                        `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                        `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '社团名称',
                        `leader_id` bigint unsigned DEFAULT NULL,
                        `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '社团描述',
                        `logo_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '社团logo地址',
                        `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1启用 0禁用',
                        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        `booking_numbers` int DEFAULT '0' COMMENT '待开始预约时间',
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社团表';

-- 6. 场地表
CREATE TABLE `venue` (
                         `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                         `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '场地名称',
                         `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '位置',
                         `capacity` int unsigned DEFAULT NULL COMMENT '容量，仅展示',
                         `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
                         `status` tinyint NOT NULL DEFAULT '1' COMMENT '1启用 0禁用',
                         `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场地表';

-- 7. 场地时段表
CREATE TABLE `venue_slot` (
                              `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                              `venue_id` bigint unsigned NOT NULL COMMENT '场地ID（逻辑外键，venue 表未列出）',
                              `slot_date` date NOT NULL COMMENT '日期',
                              `start_time` time NOT NULL,
                              `end_time` time NOT NULL,
                              `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN' COMMENT '时段状态：OPEN/BOOKED/CLOSED',
                              `capacity` int unsigned NOT NULL DEFAULT '0',
                              `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_slot_venue_date_start` (`venue_id`,`slot_date`,`start_time`),
                              KEY `idx_slot_date` (`slot_date`),
                              KEY `idx_date_status` (`slot_date`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场地时段表';

-- 8. 活动表
CREATE TABLE `activity` (
                            `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                            `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                            `summary` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '活动摘要',
                            `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '详细描述',
                            `cover_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图URL',
                            `venue_text` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '场地文本描述',
                            `start_time` datetime DEFAULT NULL COMMENT '活动开始时间',
                            `end_time` datetime DEFAULT NULL COMMENT '活动结束时间',
                            `club_id` bigint unsigned NOT NULL COMMENT '社团ID（逻辑外键，club 表未列出）',
                            `venue_id` bigint unsigned DEFAULT NULL COMMENT '场地ID（逻辑外键，venue 表未列出）',
                            `capacity` int unsigned NOT NULL DEFAULT '0' COMMENT '总名额',
                            `remaining` int unsigned NOT NULL DEFAULT '0' COMMENT '剩余名额，扣减用条件更新，不做 version 锁',
                            `waitlist_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否开启候补 0/1',
                            `waitlist_limit` int unsigned NOT NULL DEFAULT '50' COMMENT '候补上限，最大200',
                            `waitlist_Remaining` int DEFAULT NULL,
                            `signup_start` datetime NOT NULL COMMENT '报名开始时间',
                            `signup_end` datetime NOT NULL COMMENT '报名结束时间',
                            `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '草稿' COMMENT '活动状态：草稿/待审/报名中/已满/结束/取消',
                            `reject_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '驳回原因',
                            `created_by` bigint unsigned NOT NULL DEFAULT '0' COMMENT '创建人ID',
                            `version` int unsigned NOT NULL DEFAULT '0' COMMENT '乐观锁：仅管理员编辑活动信息时使用',
                            `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            `is_archive` tinyint(1) NOT NULL DEFAULT '0',
                            PRIMARY KEY (`id`),
                            KEY `idx_activity_status_start_id` (`status`,`signup_start`,`id`),
                            KEY `idx_activity_club` (`club_id`),
                            KEY `idx_activity_venue` (`venue_id`),
                            KEY `idx_start` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动表';

-- 9. 活动报名表
CREATE TABLE `registration` (
                                `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                `activity_id` bigint unsigned NOT NULL,
                                `user_id` bigint unsigned NOT NULL,
                                `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '待定' COMMENT '报名状态：待定/成功/取消/候补',
                                `ticket_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '票号，业务上唯一',
                                `cancel_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '取消原因',
                                `cancelled_by` bigint unsigned DEFAULT NULL COMMENT '取消操作人',
                                `idempotency_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '客户端幂等键，防重复提交',
                                `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `is_archive` tinyint(1) NOT NULL DEFAULT '0',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_reg_activity_user` (`activity_id`,`user_id`),
                                UNIQUE KEY `uk_reg_idempotency` (`idempotency_key`),
                                UNIQUE KEY `uk_reg_ticket_no` (`ticket_no`),
                                KEY `idx_reg_user` (`user_id`),
                                KEY `idx_activity_status` (`activity_id`,`status`,`created_at`),
                                KEY `idx_user_created` (`user_id`,`created_at`),
                                CONSTRAINT `fk_reg_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`),
                                CONSTRAINT `fk_reg_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动报名表';

-- 10. 场地预约表
CREATE TABLE `booking` (
                           `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                           `slot_id` bigint unsigned NOT NULL,
                           `user_id` bigint unsigned NOT NULL,
                           `club_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '社团ID',
                           `purpose` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '用途',
                           `idempotency_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '幂等键',
                           `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '草稿' COMMENT '预约状态：草稿/待审/成功/取消',
                           `cancel_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '取消原因',
                           `cancelled_by` bigint unsigned DEFAULT NULL COMMENT '取消操作人',
                           `reject_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '取消原因',
                           `version` int unsigned NOT NULL DEFAULT '0' COMMENT '乐观锁：预约状态流转时使用',
                           `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `uk_booking_slot_user` (`slot_id`,`user_id`),
                           UNIQUE KEY `uk_booking_idem` (`user_id`,`idempotency_key`),
                           UNIQUE KEY `uk_booking_idempotency` (`idempotency_key`),
                           KEY `idx_booking_user` (`user_id`),
                           KEY `idx_club_status` (`club_id`,`status`),
                           KEY `idx_slot_status` (`slot_id`,`status`),
                           CONSTRAINT `fk_booking_slot` FOREIGN KEY (`slot_id`) REFERENCES `venue_slot` (`id`),
                           CONSTRAINT `fk_booking_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场地预约表';

-- 11. 签到表
CREATE TABLE `checkin` (
                           `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                           `registration_id` bigint unsigned DEFAULT NULL COMMENT '报名ID',
                           `activity_id` bigint unsigned NOT NULL,
                           `user_id` bigint unsigned NOT NULL,
                           `status` tinyint NOT NULL DEFAULT '1' COMMENT '1已签到 2已撤销',
                           `checked_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
                           `operator_id` bigint unsigned DEFAULT NULL COMMENT '操作人（工作人员）',
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `uk_checkin_activity_user` (`activity_id`,`user_id`),
                           UNIQUE KEY `uk_checkin_registration` (`registration_id`),
                           KEY `idx_checkin_user` (`user_id`),
                           CONSTRAINT `fk_checkin_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`),
                           CONSTRAINT `fk_checkin_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到表';

-- 12. 站内通知表
CREATE TABLE `notification` (
                                `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                `user_id` bigint unsigned NOT NULL,
                                `message_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '消息ID',
                                `type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知类型',
                                `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                `read_status` tinyint NOT NULL DEFAULT '0' COMMENT '0未读 1已读',
                                `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_notification_message_id` (`message_id`),
                                KEY `idx_notification_user_read_id` (`user_id`,`read_status`,`id`),
                                CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内通知表';

-- 13. 事务性发件箱
CREATE TABLE `outbox_message` (
                                  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                  `message_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '消息ID',
                                  `message_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息类型，如 RegistrationCreated',
                                  `aggregate_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '聚合类型，如ACTIVITY/REGISTRATION',
                                  `aggregate_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '聚合ID',
                                  `payload` json NOT NULL COMMENT '消息负载',
                                  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '待投递' COMMENT '投递状态：待投递/成功/失败',
                                  `retry_count` int unsigned NOT NULL DEFAULT '0',
                                  `next_retry_at` datetime DEFAULT NULL COMMENT '下次重试时间',
                                  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_outbox_event_id` (`message_id`),
                                  KEY `idx_outbox_status_next` (`status`,`next_retry_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事务性发件箱';

-- 14. 已处理消息表
CREATE TABLE `processed_message` (
                                     `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                     `message_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息ID',
                                     `consumer` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消费者标识',
                                     `processed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_event_consumer` (`message_id`,`consumer`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='已处理消息表（消费端幂等去重）';

SET FOREIGN_KEY_CHECKS = 1;