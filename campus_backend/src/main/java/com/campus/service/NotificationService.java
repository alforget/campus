package com.campus.service;

import com.campus.entitys.Notification;
import com.campus.entitys.PageBean;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

import java.io.IOException;

public interface NotificationService {
    // 处理通知消息
    void handleNotification(String payload, Message message, Channel channel) throws IOException;

    //获取本人通知列表 （可根据readStatus查询未读或已读通知）
    PageBean<Notification> list(Integer pageNum, Integer pageSize, Long userId, Integer readStatus);
    // 标记通知为已读
    void read(Long id);
    // 标记所有通知为已读
    void readAll(Long userId);
}
