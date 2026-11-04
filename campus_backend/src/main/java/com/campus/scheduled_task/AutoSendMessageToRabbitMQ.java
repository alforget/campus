package com.campus.scheduled_task;

import com.campus.config.RabbitMQConfig;
import com.campus.entitys.OutboxMessage;
import com.campus.service.OutboxMessageService;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AutoSendMessageToRabbitMQ {
    @Autowired
    private OutboxMessageService outboxMessageService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 自动检测并发送消息到 RabbitMQ
    @Scheduled(fixedDelay = 5000)// 每5秒执行一次
    public void sendMessageToRabbitMQ() {
        // 从出箱消息表中查询所有未处理的消息
        List<OutboxMessage> messages = outboxMessageService.findPendingOrFailed();
        // 遍历消息列表，将每个消息发送到 RabbitMQ
        for (OutboxMessage message : messages) {
            // 构建 CorrelationData，将 messageId 作为关联 ID
            CorrelationData correlationData = new CorrelationData(message.getMessageId());

            // 发送消息
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                    message.getPayload(),
                    msg -> {
                        // 🚨 核心：把 Outbox 里的 messageId 塞进 MQ 的消息属性中
                        msg.getMessageProperties().setMessageId(message.getMessageId());
                        return msg;
                    },
                    correlationData);
        }
    }
}
