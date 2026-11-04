package com.campus.entitys;
/**
 * 通知实体
 */

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class Notification {
    private Long id;                  // 主键
    private Long userId;              // 接收用户ID
    private String messageId;           // 消息ID（新增）
    private String type;              // 通知类型，报名成功、进入候补、候补转正、报名取消、活动取消、活动审核通过/驳回、预约成功/取消
    private String title;             // 标题
    private String content;           // 内容
    private Integer readStatus;       // 0未读 1已读
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间（新增）
}