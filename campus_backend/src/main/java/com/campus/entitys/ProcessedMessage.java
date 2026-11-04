package com.campus.entitys;
/**
 * 已处理消息表
 */

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class ProcessedMessage {
    private Long id;                  // 主键
    private String messageId;           // 消息ID
    private String consumer;          // 消费者标识
    private LocalDateTime processedAt;// 处理时间
}
