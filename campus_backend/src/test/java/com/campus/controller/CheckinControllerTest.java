package com.campus.controller;

import com.campus.entitys.*;
import com.campus.exception.BusinessException;
import com.campus.service.*;
import com.campus.utils.CurrentUserInfoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckinControllerTest {

    @Mock private CheckinService checkinService;
    @Mock private RegistrationService registrationService;
    @Mock private ActivityService activityService;
    @Mock private RoleService roleService;
    @Mock private UserService userService;

    @InjectMocks
    private CheckinController checkinController;

    // 公共前置数据准备
    private void setupCommonMocks(Long userId, Long activityId, String ticketNo, Long clubId) {
        // 1. 模拟角色和用户
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setClubId(clubId);

        when(roleService.getRoleId("clubAdmin")).thenReturn(2L);
        when(userService.findById(userId)).thenReturn(mockUser);

        // 2. 模拟报名记录
        Registration mockReg = new Registration();
        mockReg.setId(999L);
        mockReg.setActivityId(activityId);
        mockReg.setUserId(userId);
        when(registrationService.findByUserIdAndTicketNo(userId, ticketNo)).thenReturn(mockReg);
    }

    // ==========================================
    // 测试 1：开始前 30 分钟外拒绝
    // ==========================================
    @Test
    void testCheckin_Before30Min_ShouldReject() {
        Long userId = 1001L;
        Long activityId = 100L;
        String ticketNo = "TK-123";

        setupCommonMocks(userId, activityId, ticketNo, 50L);

        // 🚨 模拟活动在 1 小时后才开始（不在 30 分钟内）
        Activity mockActivity = new Activity();
        mockActivity.setClubId(50L);
        mockActivity.setStartTime(LocalDateTime.now().plusHours(1));
        mockActivity.setEndTime(LocalDateTime.now().plusHours(2));
        when(activityService.findById(activityId)).thenReturn(mockActivity);

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getRoleId).thenReturn(2L);
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(userId);

            // 执行并断言
            BusinessException ex = assertThrows(BusinessException.class, () -> {
                checkinController.checkin(ticketNo);
            });
            assertEquals("签到未开始，无法签到", ex.getMessage());

            // 验证：没有去写签到表
            verify(checkinService, never()).add(anyLong(), anyLong(), anyLong());
        }
    }

    // ==========================================
    // 测试 2：结束后拒绝
    // ==========================================
    @Test
    void testCheckin_AfterEndTime_ShouldReject() {
        Long userId = 1001L;
        Long activityId = 100L;
        String ticketNo = "TK-123";

        setupCommonMocks(userId, activityId, ticketNo, 50L);

        // 🚨 模拟活动在 1 小时前就结束了
        Activity mockActivity = new Activity();
        mockActivity.setClubId(50L);
        mockActivity.setStartTime(LocalDateTime.now().minusHours(2));
        mockActivity.setEndTime(LocalDateTime.now().minusHours(1));
        when(activityService.findById(activityId)).thenReturn(mockActivity);

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getRoleId).thenReturn(2L);
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(userId);

            BusinessException ex = assertThrows(BusinessException.class, () -> {
                checkinController.checkin(ticketNo);
            });
            assertEquals("签到已结束，无法签到", ex.getMessage());

            verify(checkinService, never()).add(anyLong(), anyLong(), anyLong());
        }
    }

    // ==========================================
    // 测试 3：重复签到 409 (已签到)
    // ==========================================
    @Test
    void testCheckin_AlreadyCheckedIn_ShouldReject() {
        Long userId = 1001L;
        Long activityId = 100L;
        String ticketNo = "TK-123";

        setupCommonMocks(userId, activityId, ticketNo, 50L);

        // 模拟活动时间正常（正在进行中）
        Activity mockActivity = new Activity();
        mockActivity.setClubId(50L);
        mockActivity.setStartTime(LocalDateTime.now().minusMinutes(10));
        mockActivity.setEndTime(LocalDateTime.now().plusMinutes(50));
        when(activityService.findById(activityId)).thenReturn(mockActivity);

        // 🚨 模拟数据库里已经有一条签到记录了
        when(checkinService.findByRegistrationId(999L)).thenReturn(new Checkin());

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getRoleId).thenReturn(2L);
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(userId);

            BusinessException ex = assertThrows(BusinessException.class, () -> {
                checkinController.checkin(ticketNo);
            });
            // 这里对应你业务代码里的“409”报错
            assertEquals("您已签到，无法重复签到", ex.getMessage());

            verify(checkinService, never()).add(anyLong(), anyLong(), anyLong());
        }
    }

    // ==========================================
    // 测试 4：正常签到成功 (补充正向用例)
    // ==========================================
    @Test
    void testCheckin_Success() {
        Long userId = 1001L;
        Long activityId = 100L;
        String ticketNo = "TK-123";

        setupCommonMocks(userId, activityId, ticketNo, 50L);

        Activity mockActivity = new Activity();
        mockActivity.setClubId(50L);
        mockActivity.setStartTime(LocalDateTime.now().minusMinutes(10));
        mockActivity.setEndTime(LocalDateTime.now().plusMinutes(50));
        when(activityService.findById(activityId)).thenReturn(mockActivity);

        // 🚨 模拟没签到过
        when(checkinService.findByRegistrationId(999L)).thenReturn(null);
        // 模拟插入签到记录成功
        when(checkinService.add(userId, activityId, 999L)).thenReturn(1);
        // 模拟更新报名状态成功
        when(registrationService.updateStatus(999L, "已签到")).thenReturn(1);

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getRoleId).thenReturn(2L);
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(userId);

            Result result = checkinController.checkin(ticketNo);

            assertNotNull(result);
            // 验证轨迹
            verify(checkinService).add(userId, activityId, 999L);
            verify(registrationService).updateStatus(999L, "已签到");
        }
    }
}