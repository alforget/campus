package com.campus.entitys;
/**
 * 用户实体
 */

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class User {
    private Long id;                 // 主键
    private String username;         // 登录名，唯一
    @JsonIgnore
    private String passwordHash;     // 密码哈希（BCrypt）
    private String nickname;         // 昵称
    private String email;            // 邮箱
    private String phone;            // 手机号
    private String avatarUrl;        // 头像URL
    private Long roleId;             // 角色ID（关联 role 表）
    private String role;             // 角色编码（派生/可选冗余，如 STUDENT/CLUB_ADMIN/VENUE_ADMIN/ADMIN）
    private Long clubId;             // 社团ID，CLUB_ADMIN必填，可空
    private Integer status;          // 1启用 0禁用
    private LocalDateTime lastLoginAt; // 最后登录时间
    private LocalDateTime createdAt;   // 创建时间
    private LocalDateTime updatedAt;   // 更新时间
}