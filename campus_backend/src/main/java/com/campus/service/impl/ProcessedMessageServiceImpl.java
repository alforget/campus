package com.campus.service.impl;

import com.campus.mapper.ProcessedMessageMapper;
import com.campus.service.ProcessedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProcessedMessageServiceImpl implements ProcessedMessageService {
    @Autowired
    private ProcessedMessageMapper processedMessageMapper;

    // 插入已处理记录
    @Override
    public void add(String messageId,String consumer) {
        processedMessageMapper.add(messageId,consumer);
    }
    // 根据消息ID检查是否存在已处理记录
    @Override
    public int existsById(String messageId,String consumer) {
        return processedMessageMapper.existsById(messageId,consumer);
    }
}
