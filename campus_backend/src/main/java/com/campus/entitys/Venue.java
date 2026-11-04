package com.campus.entitys;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class Venue {
    private Long id;                  // 主键
    private String name;              // 场地名称
    private String location;          // 位置
    private Integer capacity;         // 容量，仅展示
    private String description;       // 描述
    private Integer status;           // 1启用 0禁用
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间
}
