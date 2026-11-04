package com.campus.integration_test;

import com.campus.entitys.Activity;
import com.campus.entitys.Registration;
import com.campus.mapper.RegistrationMapper;
import com.campus.service.ActivityService;
import com.campus.service.RoleService;
import com.campus.utils.CurrentUserInfoUtil;
import com.campus.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private JwtUtil jwtUtil;
    @MockBean private StringRedisTemplate stringRedisTemplate;
    @MockBean private RoleService roleService;
    @MockBean private ActivityService activityService;
    @MockBean private RegistrationMapper registrationMapper;

    @Test
    void testStudentAccessAdminApi_ShouldReturn403() throws Exception {
        String studentToken = "token-student-abc";
        Map<String, Object> studentClaims = new HashMap<>();
        studentClaims.put("userId", 200L);
        studentClaims.put("roleId", 3L);

        when(jwtUtil.parseToken(studentToken)).thenReturn(studentClaims);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get("login:token:200")).thenReturn(studentToken);
        when(roleService.getRoleId("admin")).thenReturn(1L);

        mockMvc.perform(get("/activity/list")
                        .header("Authorization", studentToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value("没有权限执行此操作"));
    }

    @Test
    void testUserA_UpdateUserBActivity_ShouldForbiddenOrFail() throws Exception {
        String tokenA = "token-clubAdmin-A";
        Long userAId = 100L;

        Map<String, Object> claimsA = new HashMap<>();
        claimsA.put("userId", userAId);
        claimsA.put("roleId", 2L);
        when(jwtUtil.parseToken(tokenA)).thenReturn(claimsA);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get("login:token:" + userAId)).thenReturn(tokenA);
        when(roleService.getRoleId("clubAdmin")).thenReturn(2L);

        Activity activityB = new Activity();
        activityB.setId(999L);
        activityB.setCreatedBy(200L);
        activityB.setStatus("草稿");
        activityB.setVersion(1);
        when(activityService.findById(999L)).thenReturn(activityB);

        Activity updateReq = new Activity();
        updateReq.setId(999L);
        updateReq.setTitle("A想偷偷篡改B的活动");
        updateReq.setSummary("这是一个用于集成测试的迎新晚会摘要");
        updateReq.setDescription("这是一个用于集成测试的详细描述，包含了非常多的信息，长度绝对超过二十个字符。");
        updateReq.setCoverUrl("https://example.com/cover.jpg");
        updateReq.setVenueId(10L);
        updateReq.setStartTime(LocalDateTime.now().plusDays(2));
        updateReq.setEndTime(LocalDateTime.now().plusDays(2).plusHours(2));
        updateReq.setSignupStart(LocalDateTime.now().plusDays(1));
        updateReq.setSignupEnd(LocalDateTime.now().plusDays(1).plusHours(5));
        updateReq.setCapacity(100);
        updateReq.setWaitlistEnabled(1);
        updateReq.setWaitlistLimit(100);
        updateReq.setVersion(1);

        when(activityService.updatedetail(any(Activity.class), eq(userAId))).thenReturn(0);

        mockMvc.perform(put("/activity/update")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("更新活动失败,请重新尝试"));
    }

    @Test
    void testUserA_CancelUserBRegistration_ShouldForbidden() throws Exception {
        String tokenStudentA = "token-student-A";
        Long studentAId = 300L;
        Long studentBId = 400L;

        Map<String, Object> claimsA = new HashMap<>();
        claimsA.put("userId", studentAId);
        // 🚨 核心修改：加上对 roleService 的 Mock，确保拿到 3L
        when(roleService.getRoleId("student")).thenReturn(3L);
        claimsA.put("roleId", 3L);
        when(jwtUtil.parseToken(tokenStudentA)).thenReturn(claimsA);

        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get("login:token:" + studentAId)).thenReturn(tokenStudentA);

        Registration regB = new Registration();
        regB.setId(888L);
        regB.setUserId(studentBId);
        regB.setActivityId(100L); // 补上活动ID防止 NPE

        when(registrationMapper.findById(888L)).thenReturn(regB);

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = Mockito.mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(studentAId);

            mockMvc.perform(put("/registration/cancel")
                            .header("Authorization", tokenStudentA)
                            .param("registrationId", "888")
                            .param("cancelReason", "我就要取消别人的"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.msg").value("没有权限执行此操作"));

            verify(registrationMapper, never()).cancel(anyLong(), anyLong(), anyString());
        }
    }

    @Test
    void testUpload_FakeExtension_ShouldReject() throws Exception {
        String token = "token-user-100";
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 100L);
        claims.put("roleId", 3L);

        when(jwtUtil.parseToken(token)).thenReturn(claims);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get("login:token:100")).thenReturn(token);

        MockMultipartFile fakeFile = new MockMultipartFile(
                "file",
                "hack_script.jpg",
                "application/octet-stream",
                "This is a fake image, actually it is a malicious script!".getBytes()
        );

        mockMvc.perform(multipart("/upload/uploadAvatar")
                        .file(fakeFile)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").exists());
    }
}