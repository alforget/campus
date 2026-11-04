package com.campus.service;

import org.apache.ibatis.annotations.Insert;

public interface ProcessedMessageService {
    // 根据消息ID检查是否存在已处理记录
    int existsById(String messageId,String consumer);
    // 插入已处理记录
    void add(String messageId,String consumer);
}
