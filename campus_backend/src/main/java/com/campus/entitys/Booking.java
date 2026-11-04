package com.campus.entitys;
/**
 * 预约实体
 */

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class Booking {
    private Long id;                  // 主键
    private Long slotId;              // 场地时段ID
    private Long userId;              // 用户ID
    private Long clubId;              // 社团ID（新增）
    private String purpose;           // 用途（新增）
    private String status;            // 已通过/已取消
    private String idempotencyKey;    // 幂等键（新增） 防止重复预约
    private String cancelReason;      // 取消原因（新增）
    private Long cancelledBy;         // 取消操作人（新增）
    private String rejectReason;      // 驳回原因
    private Integer version;          // 乐观锁，状态流转时使用（现有）
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间
}