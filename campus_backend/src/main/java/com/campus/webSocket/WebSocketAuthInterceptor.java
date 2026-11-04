package com.campus.webSocket;

import com.campus.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 从 URL 获取 token，例如 ws://localhost:8080/ws/notify?token=eyJhbG...
        String query = request.getURI().getQuery();
        if (query == null || !query.startsWith("token=")) {
            return false;
        }
        String token = query.substring(6);

        try {
            Map<String, Object> claims = jwtUtil.parseToken(token);
            Long userId = Long.valueOf(claims.get("userId").toString());

            // 校验 Redis 里的 Token 是否一致（和你登录逻辑一样）
            String redisKey = "login:token:" + userId;
            String redisToken = stringRedisTemplate.opsForValue().get(redisKey);
            if (redisToken == null || !redisToken.equals(token)) {
                return false;
            }

            // 将 userId 存入 session 属性中，后续发消息要用
            attributes.put("userId", userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}