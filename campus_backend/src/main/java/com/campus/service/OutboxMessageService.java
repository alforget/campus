package com.campus.service;

import com.campus.entitys.OutboxMessage;

import java.util.List;

public interface OutboxMessageService {
    // 根据状态查询所有消息
    List<OutboxMessage> findPendingOrFailed();

    // 更新消息状态
    void updateStatus(String messageId, String status);
    // 添加消息
    int add(String messageId, String messageType, String payload);
}
