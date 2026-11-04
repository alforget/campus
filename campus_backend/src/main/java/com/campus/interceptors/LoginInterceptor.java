package com.campus.interceptors;

import com.campus.utils.JwtUtil;
import com.campus.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private JwtUtil jwtUtil;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if(token == null){
            return false;
        }
        try{
            //解析token,获取userId
            Map<String, Object> claims = jwtUtil.parseToken(token);
            Long userId = Long.valueOf(claims.get("userId").toString());
            //从redis中获取token
            String redisKey = "login:token:" + userId;
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            String redisToken = operations.get(redisKey);
            if(redisToken == null||!redisToken.equals(token)){
                throw new Exception("token验证失败");
            }
            //将claims存储到ThreadLocal中
            ThreadLocalUtil.set(claims);
            //放行
            return true;
        }catch (Exception e){
            response.setStatus(401);
            System.out.println("拦截器异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //从ThreadLocal中移除claims数据
        ThreadLocalUtil.remove();
    }
}
