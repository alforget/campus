package com.campus.webSocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotifyWebSocketHandler extends TextWebSocketHandler {

    // 存储在线用户的连接。一个用户可能在手机和电脑同时登录，所以用 Set 存多个 session
    public static final Map<Long, Set<WebSocketSession>> SESSION_POOL = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        // 建立连接时，存入连接池
        SESSION_POOL.computeIfAbsent(userId, k -> Collections.synchronizedSet(new HashSet<>())).add(session);
        System.out.println("用户 " + userId + " 已连接 WebSocket");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        // 断开连接时，移除连接
        Set<WebSocketSession> sessions = SESSION_POOL.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                SESSION_POOL.remove(userId);
            }
        }
        System.out.println("用户 " + userId + " 断开 WebSocket");
    }
}