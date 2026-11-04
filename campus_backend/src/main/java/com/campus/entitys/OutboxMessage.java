package com.campus.entitys;
/**
 * 外部消息实体
 *  事务性发件箱
 **/
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class OutboxMessage {
    private Long id;                  // 主键
    private String messageId;           // 消息ID，唯一（新增）
    private String messageType;         // 消息类型
    private String aggregateType;     // 聚合类型（新增）
    private Long aggregateId;         // 聚合ID（新增）
    private String payload;           // 消息负载（JSON）
    private String status;            // 待投递/已投递/失败
    private Integer retryCount;       // 已重试次数
    private LocalDateTime nextRetryAt;// 下次重试时间
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间
}
