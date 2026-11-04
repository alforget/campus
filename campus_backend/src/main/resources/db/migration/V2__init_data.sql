-- V2__init_data.sql
-- 初始化系统基础数据

-- 1. 插入基础角色数据 (显式指定 ID 为 1~4)
INSERT  INTO `role` (`id`, `code`, `name`) VALUES
                                              (1, '001', 'admin'),
                                              (2, '002', 'clubAdmin'),
                                              (3, '003', 'venueAdmin'),
                                              (4, '004', 'student');

-- 2. 插入超级管理员用户 (role_id = 1) 密码为 123456
INSERT  INTO `user` (`username`, `password_hash`, `role_id`, `status`) VALUES
    ('admin', '$2a$10$rUuWK71IAPeVNDFl2MZlo.IN27HjMUwQ3R7PPtcHRDMueKmsEihLW', 1, 1);