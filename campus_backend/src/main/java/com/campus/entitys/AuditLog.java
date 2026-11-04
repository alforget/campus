package com.campus.entitys;
/**
 * 审计日志实体
 */

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class AuditLog {
    private Long id;                  // 主键
    private Long actorId;             // 操作人ID（原 user_id 重命名）
    private String action;            // 动作，如 activity.update
    private String targetType;        // 目标类型
    private Long targetId;            // 目标ID
    private String detail;            // 变更详情（JSON）
    private String ip;                // 操作IP
    private LocalDateTime createdAt;  // 创建时间
}
