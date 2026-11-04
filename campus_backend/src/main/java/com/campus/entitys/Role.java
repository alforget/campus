package com.campus.entitys;
/**
 * 角色实体
 */

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class Role {
    private Long id;                // 主键ID
    private String code;            // 角色编码，如 admin/student，全局唯一
    private String name;            // 角色名称
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}
