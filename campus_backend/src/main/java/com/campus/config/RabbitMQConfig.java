package com.campus.config;

import com.campus.service.OutboxMessageService;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private OutboxMessageService outboxMessageService;

    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_DLQ = "notification.dlq";
    public static final String NOTIFICATION_DLX = "notification.dlx";
    public static final String NOTIFICATION_ROUTING_KEY = "notify.routing.key";

    // 1. 声明业务交换机、队列，绑定死信交换机
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", "dlq.routing.key")
                .build();
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(notificationExchange()).with(NOTIFICATION_ROUTING_KEY);
    }

    // 2. 声明死信交换机、死信队列
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(NOTIFICATION_DLX);
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqQueue()).to(dlxExchange()).with(NOTIFICATION_ROUTING_KEY);
    }

    @PostConstruct
    public void init() {
        // 1. 消息到达 Exchange 的回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData == null) return;
            String messageId = correlationData.getId(); // 传入的消息唯一ID

            if (ack) {
                // MQ 确认收到，更新为已投递
                outboxMessageService.updateStatus(messageId, "已投递");
                System.out.println("消息 " + messageId + " 投递成功");
            } else {
                // MQ 拒收（比如交换机满了、宕机），标记为失败，等定时任务重试
                outboxMessageService.updateStatus(messageId, "失败");
                System.err.println("消息 " + messageId + " 投递失败，原因：" + cause);
            }
        });
        // 2. 🚨 补充：消息从 Exchange 路由到 Queue 失败的回调（比如路由键写错）
        rabbitTemplate.setReturnsCallback(returned -> {
            String messageId = returned.getMessage().getMessageProperties().getMessageId(); // 或者从 CorrelationData 拿
            System.err.println("消息未能路由到队列，原因：" + returned.getReplyText());
            // 这种情况下，需要把状态改回未投递，让定时任务重试
            if (messageId != null) {
                outboxMessageService.updateStatus(messageId, "失败");
            }
        });
    }
}