package com.campus.service.impl;

import com.campus.entitys.OutboxMessage;
import com.campus.mapper.OutboxMessageMapper;

import com.campus.service.OutboxMessageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class OutboxMessageServiceImpl implements OutboxMessageService {
    @Autowired
    private OutboxMessageMapper outboxMessageMapper;
    // 根据状态查询所有消息
    @Override
    public List<OutboxMessage> findPendingOrFailed() {
        return outboxMessageMapper.findPendingOrFailed();
    }
    // 更新消息状态
    @Override
    public void updateStatus(String messageId, String newStatus) {
        outboxMessageMapper.updateStatus(messageId, newStatus);
    }
    // 添加消息
    @Override
    public int add(String messageId, String messageType, String payload) {
        return outboxMessageMapper.add(messageId, messageType, payload);
    }
}
