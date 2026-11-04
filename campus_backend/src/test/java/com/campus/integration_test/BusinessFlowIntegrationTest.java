package com.campus.integration_test;

import com.campus.entitys.Activity;
import com.campus.entitys.Result;
import com.campus.entitys.User;
import com.campus.entitys.VenueSlot;
import com.campus.exception.ErrorCode;
import com.campus.service.*;
import com.campus.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusinessFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // 把所有外部依赖和 Service 全部 Mock 掉，只测 Controller + 拦截器逻辑
    @MockBean private ActivityService activityService;
    @MockBean private UserService userService;
    @MockBean private RoleService roleService;
    @MockBean private VenueSlotService venueSlotService;
    @MockBean private BookingService bookingService;
    @MockBean private RegistrationService registrationService;
    @MockBean private OutboxMessageService outboxMessageService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private StringRedisTemplate stringRedisTemplate;

    @Test
    void testFullBusinessFlow() throws Exception {
        // 模拟拦截器依赖的 Redis 操作
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);

        // ==========================================
        // 准备阶段：伪造三种身份的 Token 和 Claims
        // ==========================================
        String clubAdminToken = "token-clubAdmin";
        String adminToken = "token-admin";
        String studentToken = "token-student";

        // 1. 社团管理员 (ID: 100, ClubID: 50)
        Map<String, Object> clubAdminClaims = new HashMap<>();
        clubAdminClaims.put("userId", 100L);
        clubAdminClaims.put("roleId", 2L);
        when(jwtUtil.parseToken(clubAdminToken)).thenReturn(clubAdminClaims);
        when(ops.get("login:token:100")).thenReturn(clubAdminToken);
        when(roleService.getRoleId("clubAdmin")).thenReturn(2L);

        User clubAdminUser = new User();
        clubAdminUser.setId(100L);
        clubAdminUser.setClubId(50L);
        when(userService.fingByName(anyString())).thenReturn(clubAdminUser);
        when(userService.findById(100L)).thenReturn(clubAdminUser);

        // 2. 系统管理员 (ID: 1)
        Map<String, Object> adminClaims = new HashMap<>();
        adminClaims.put("userId", 1L);
        adminClaims.put("roleId", 1L);
        when(jwtUtil.parseToken(adminToken)).thenReturn(adminClaims);
        when(ops.get("login:token:1")).thenReturn(adminToken);
        when(roleService.getRoleId("admin")).thenReturn(1L);

        // 3. 学生 (ID: 200)
        Map<String, Object> studentClaims = new HashMap<>();
        studentClaims.put("userId", 200L);
        studentClaims.put("roleId", 3L);
        when(jwtUtil.parseToken(studentToken)).thenReturn(studentClaims);
        when(ops.get("login:token:200")).thenReturn(studentToken);

        // ==========================================
        // 步骤 1：社团管理员建活动
        // ==========================================
        Activity newActivity = new Activity();
        newActivity.setId(999L);
        // 1. 标题：长度必须在 5-50 之间
        newActivity.setTitle("测试用迎新晚会活动");
        // 2. 摘要：不能为空，长度 10-200
        newActivity.setSummary("这是一个用于集成测试的迎新晚会摘要");
        // 3. 详细描述：不能为空，长度 20-5000
        newActivity.setDescription("这是一个用于集成测试的详细描述，包含了非常多的信息，长度绝对超过二十个字符。");
        newActivity.setCoverUrl("https://example.com/cover.jpg"); // 满足 @URL 格式

        newActivity.setVenueId(10L);
        newActivity.setStartTime(LocalDateTime.now().plusDays(2));
        newActivity.setEndTime(LocalDateTime.now().plusDays(2).plusHours(2));
        newActivity.setSignupStart(LocalDateTime.now().plusDays(1));
        newActivity.setSignupEnd(LocalDateTime.now().plusDays(1).plusHours(5));

        // 4. 名额：1-500
        newActivity.setCapacity(100);
        newActivity.setRemaining(100);
        // 5. 候补开关：0 或 1
        newActivity.setWaitlistEnabled(1);
        // 6. 候补上限：50-200（刚才填的 50 刚好踩线，这里改成 100 安全点）
        newActivity.setWaitlistLimit(100);
        // 模拟场地时段存在且未被占用
        VenueSlot mockSlot = new VenueSlot();
        mockSlot.setId(10L);
        when(venueSlotService.findByVenueIdAndTime(anyLong(), any(), any(), any()))
                .thenReturn(Collections.singletonList(mockSlot));
        when(bookingService.findByVenueSlotIdAndStatus(anyLong(), eq("BOOKED"))).thenReturn(null);

        mockMvc.perform(post("/activity/add")
                        .header("Authorization", clubAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newActivity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SUCCESS.getCode())); // 假设 1 代表成功

        verify(activityService).add(any(Activity.class));

        // ==========================================
        // 步骤 2：社团管理员提交活动
        // ==========================================
        // 模拟数据库里查出来的活动是草稿状态
        Activity draftActivity = new Activity();
        draftActivity.setId(999L);
        draftActivity.setStatus("草稿");
        when(activityService.findById(999L)).thenReturn(draftActivity);
        when(activityService.submit(eq(999L), eq(100L), eq("待审核"))).thenReturn(1);

        mockMvc.perform(patch("/activity/submit")
                        .header("Authorization", clubAdminToken)
                        .param("id", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SUCCESS.getCode()));


        verify(activityService).submit(999L, 100L, "待审核");

        // ==========================================
        // 步骤 3：系统管理员审核通过活动
        // ==========================================
        // 模拟现在活动状态变成了待审核
        Activity pendingActivity = new Activity();
        pendingActivity.setId(999L);
        pendingActivity.setStatus("待审核");
        pendingActivity.setTitle("迎新晚会");
        pendingActivity.setCreatedBy(100L);
        when(activityService.findById(999L)).thenReturn(pendingActivity);


        when(outboxMessageService.add(anyString(), anyString(), anyString())).thenReturn(1);

        mockMvc.perform(patch("/activity/audit")
                        .header("Authorization", adminToken)
                        .param("id", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SUCCESS.getCode()));

        // 验证状态更新为已发布，且发送了通知消息
        verify(activityService).updateStatus("已发布", 999L);
        verify(outboxMessageService).add(anyString(), eq("活动通过审核"), anyString());

        // ==========================================
        // 步骤 4：学生报名活动 (走带幂等的报名接口)
        // ==========================================
        // 模拟活动已发布，且有余额
        Activity publishedActivity = new Activity();
        publishedActivity.setId(999L);
        publishedActivity.setStatus("报名中"); // 假设系统定时任务把已发布变成了报名中
        publishedActivity.setRemaining(10);
        when(activityService.findById(999L)).thenReturn(publishedActivity);
        when(registrationService.addWithIdempotencyKey(eq(999L), anyString()))
                .thenReturn(Result.success("status=成功,ticketNo=123456"));
        // 模拟学生没有报过名
        when(registrationService.findByUserIdAndActivityId(200L, 999L)).thenReturn(null);
        // 模拟学生报名依赖的各种 Redis 和扣减逻辑
        // 根据你的接口签名，如果是 /registration/add
        mockMvc.perform(post("/registration/add")
                        .header("Authorization", studentToken)
                        .header("Idempotency-Key", "req-12345")
                        .param("activityId", "999")
                        .param("idempotencyKey", "req-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SUCCESS.getCode()));

        // 验证确实执行了报名核心逻辑
        verify(registrationService).addWithIdempotencyKey(eq(999L), eq("req-12345"));
    }
}
