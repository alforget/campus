package com.campus.entitys;
/**
 * 权限实体
 */

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class Permission {
    private Long id;                // 主键ID
    private String code;            // 权限编码，如 activity:edit，全局唯一
    private String name;            // 权限名称
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}