package com.campus.entitys;
/**
 * 报名实体
 */

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class Registration {
    private Long id;                  // 主键
    private Long activityId;          // 活动ID
    private Long userId;              // 用户ID
    private String status;            // 状态  成功/取消/候补/已签到/未出席
    private String ticketNo;          // 票号，确认后生成，唯一
    private String idempotencyKey;    // 幂等键，唯一
    private String cancelReason;      // 取消原因（新增）
    private Long cancelledBy;         // 取消操作人（新增）
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间（新增）
    private Boolean isArchive;        // 是否归档 0/1`
   }
