package com.campus.entitys;
/**
 * 角色权限关联实体
 */

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class RolePermission {
    private Long id;                // 主键ID
    private Long roleId;            // 角色ID
    private Long permissionId;      // 权限ID
    private LocalDateTime createdAt; // 创建时间
}