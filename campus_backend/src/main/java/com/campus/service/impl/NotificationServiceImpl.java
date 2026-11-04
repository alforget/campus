package com.campus.service.impl;

import com.campus.config.RabbitMQConfig;
import com.campus.entitys.Notification;
import com.campus.entitys.NotificationMessage;
import com.campus.entitys.PageBean;
import com.campus.entitys.User;
import com.campus.mapper.NotificationMapper;
import com.campus.service.NotificationService;
import com.campus.service.ProcessedMessageService;
import com.campus.service.UserService;
import com.campus.webSocket.NotifyWebSocketHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.rabbitmq.client.Channel;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Set;


@Service
public class NotificationServiceImpl implements NotificationService {
    @Autowired
    private ProcessedMessageService processedMessageService;
    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;
    @Autowired
    private UserService userService;
    public static final String CONSUMER_ID = "notification-service";

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    @Transactional
    public void handleNotification(String payload, Message message, Channel channel) throws IOException {
        String messageId = message.getMessageProperties().getMessageId();
        // 1. 幂等校验 (ProcessedMessage)
        if (processedMessageService.existsById(messageId, CONSUMER_ID) > 0) {
            return; // 处理过了，直接返回 ACK
        }

        // 2. 解析并插入 Notification
        Notification notification = new Notification();
        try {
            NotificationMessage notificationMessage = objectMapper.readValue(payload, NotificationMessage.class);
            notification.setUserId(notificationMessage.getUserId());
            notification.setMessageId(notificationMessage.getMessageId());
            notification.setType(notificationMessage.getType());
            notification.setTitle(notificationMessage.getTitle());
            notification.setContent(notificationMessage.getContent());
            notificationMapper.add(notification);
            // 3. 记录已处理
            processedMessageService.add(messageId, CONSUMER_ID);
            // 4. 发送通知
            sendWebSocketMessage(notification.getUserId(), notification.getContent());
            // 业务完全成功后，手动 ACK 消息，确保消息被确认处理
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    private void sendWebSocketMessage(Long userId, String content) {
        Set<WebSocketSession> sessions = NotifyWebSocketHandler.SESSION_POOL.get(userId);
        if (sessions != null && !sessions.isEmpty()) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        // 简单包装一下发过去
                        String jsonMsg = "{\"type\":\"NOTIFY\",\"content\":\"" + content + "\"}";
                        session.sendMessage(new TextMessage(jsonMsg));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public PageBean<Notification> list(Integer pageNum, Integer pageSize, Long userId, Integer readStatus) {
        PageBean<Notification> pageBean = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        // 分页查询
        List<Notification> notifications = notificationMapper.list(userId, readStatus);
        Page p = (Page) notifications;
        pageBean.setTotal(p.getTotal());
        pageBean.setItems(p.getResult());
        return pageBean;
    }

    @Override
    public void read(Long id) {
        notificationMapper.read(id);
    }

    @Override
    public void readAll(Long userId) {
        // 查询所有未读通知
        List<Notification> notifications = notificationMapper.list(userId, 0);
        try (SqlSession sqlSession = sqlSessionTemplate.getSqlSessionFactory().openSession(ExecutorType.BATCH, false)) {
            NotificationMapper notificationMapper = sqlSession.getMapper(NotificationMapper.class);
            // 批量更新
            int count = 0;
            for (Notification notification : notifications) {
                count++;
                notificationMapper.read(notification.getId());
                if (count % 100 == 0) {
                    sqlSession.flushStatements();
                    sqlSession.clearCache();
                }
            }
            // 最后一次提交
            sqlSession.flushStatements();
            sqlSession.commit();
        }
    }
}