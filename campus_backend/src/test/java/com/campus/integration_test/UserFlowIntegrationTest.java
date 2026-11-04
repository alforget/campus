package com.campus.integration_test;

import com.campus.Limiter.LoginRateLimiter;
import com.campus.entitys.Activity;
import com.campus.entitys.PageBean;
import com.campus.entitys.User;
import com.campus.exception.ErrorCode;
import com.campus.scheduled_task.AutoSendMessageToRabbitMQ;
import com.campus.scheduled_task.AutoUpdateActivityStatus;
import com.campus.service.*;
import com.campus.utils.JwtUtil;
import com.campus.vo.ActivityVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private RoleService roleService;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private StringRedisTemplate stringRedisTemplate;
    @MockBean private LoginRateLimiter loginRateLimiter;

    @MockBean private ActivityService activityService;
    @MockBean private ClubService clubService;
    @MockBean private VenueService venueService;

    // 🚨 终极杀招：把后台定时任务也 Mock 掉，彻底消灭脏日志
    @MockBean private AutoSendMessageToRabbitMQ autoSendMessageToRabbitMQ;
    @MockBean private AutoUpdateActivityStatus autoUpdateActivityStatus;

    @Test
    void testFullFlow_Register_Login_AccessWithToken() throws Exception {
        String username = "testStudent";
        String password = "password123";
        String mockToken = "eyJhbGciOiJIUzI1NiJ9.fake-token";

        // ==========================================
        // 第一步：注册
        // ==========================================
        when(userService.fingByName(username)).thenReturn(null);
        when(roleService.getRoleId("student")).thenReturn(3L);

        mockMvc.perform(post("/user/register")
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SUCCESS.getCode()));

        verify(userService).add(username, password, 3L);


        // ==========================================
        // 第二步：登录
        // ==========================================
        User mockUser = new User();
        mockUser.setId(10L);
        mockUser.setUsername(username);
        mockUser.setPasswordHash("hashed_pwd");
        mockUser.setStatus(1);
        mockUser.setRoleId(3L);

        when(userService.fingByName(username)).thenReturn(mockUser);
        when(loginRateLimiter.lockedSeconds(username, "127.0.0.1")).thenReturn(0L);
        when(passwordEncoder.matches(password, "hashed_pwd")).thenReturn(true);
        when(jwtUtil.genToken(anyMap())).thenReturn(mockToken);

        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(null);

        mockMvc.perform(post("/user/login")
                        .param("username", username)
                        .param("password", password)
                        .param("ip", "127.0.0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data").value(mockToken));

        verify(ops).set(eq("login:token:10"), eq(mockToken), eq(12L), any());


        // ==========================================
        // 第三步：带 Token 访问受保护接口
        // ==========================================
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 10L);
        claims.put("username", username);
        claims.put("roleId", 3L);

        when(jwtUtil.parseToken(mockToken)).thenReturn(claims);
        when(ops.get("login:token:10")).thenReturn(mockToken);

        // 🚨 核心修复：实例化一个带空列表的 PageBean，绝对不要返回 null 的 items
        PageBean<Activity> mockPage = new PageBean<>();
        mockPage.setTotal(0L);
        mockPage.setItems(new ArrayList<>()); // 给个空列表

        // 1. 拦截活动分页查询，返回刚才构造的 mockPage
        when(activityService.listForStudent(anyInt(), anyInt())).thenReturn(mockPage); // 👈 修正点

        // 2. 拦截 Controller 里的 toVoPage 方法需要查的数据
        when(clubService.list()).thenReturn(Collections.emptyList());
        when(venueService.list()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/activity/listforstudent")
                        .header("Authorization", mockToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());
    }
}