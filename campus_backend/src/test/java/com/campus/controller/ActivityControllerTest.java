package com.campus.controller;
import com.campus.entitys.Activity;
import com.campus.entitys.Result;
import com.campus.exception.BusinessException;
import com.campus.service.*;
import com.campus.utils.CurrentUserInfoUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

    @Mock private ActivityService activityService;
    @Mock private RoleService roleService;
    @Mock private OutboxMessageService outboxMessageService;
    @Mock private ObjectMapper objectMapper;

    // 把上面造的假替身全部注入到 Controller 里
    @InjectMocks
    private ActivityController activityController;

    // ==========================================
    // 测试 1：草稿 -> 提交 (成功)
    // ==========================================
    @Test
    void testSubmit_DraftStatus_ShouldSuccess() {
        Long activityId = 1L;
        Activity mockActivity = new Activity();
        mockActivity.setId(activityId);
        mockActivity.setStatus("草稿"); // 🚨 草稿状态

        // 模拟：当前是社团管理员
        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(100L);
            mockedStatic.when(CurrentUserInfoUtil::getRoleId).thenReturn(2L); // 社团管理员ID

            when(roleService.getRoleId("clubAdmin")).thenReturn(2L);
            when(activityService.findById(activityId)).thenReturn(mockActivity);
            when(activityService.submit(eq(activityId), eq(100L), eq("待审核"))).thenReturn(1); // 更新成功

            // 执行
            Result result = activityController.submit(activityId);

            // 验证
            assertNotNull(result);
            verify(activityService).submit(activityId, 100L, "待审核");
        }
    }

    // ==========================================
    // 测试 2：非草稿 -> 提交 (失败，拦截)
    // ==========================================
    @Test
    void testSubmit_PublishedStatus_ShouldThrowException() {
        Long activityId = 1L;
        Activity mockActivity = new Activity();
        mockActivity.setId(activityId);
        mockActivity.setStatus("已发布"); // 🚨 错误状态

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getRoleId).thenReturn(2L);
            when(roleService.getRoleId("clubAdmin")).thenReturn(2L);
            when(activityService.findById(activityId)).thenReturn(mockActivity);

            // 执行并捕获异常
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                activityController.submit(activityId);
            });

            // 验证
            assertEquals("非草稿和已驳回状态的活动不能提交", exception.getMessage());
            verify(activityService, never()).submit(anyLong(), anyLong(), anyString());
        }
    }

    // ==========================================
    // 测试 3：非草稿 -> 编辑 (失败，拦截)
    // ==========================================
    @Test
    void testUpdate_NotDraft_ShouldThrowException() {
        Activity updateReq = new Activity();
        updateReq.setId(1L);

        Activity dbActivity = new Activity();
        dbActivity.setId(1L);
        dbActivity.setStatus("待审核"); // 🚨 已经提交了，不能编辑

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserId).thenReturn(100L);
            mockedStatic.when(CurrentUserInfoUtil::getRoleId).thenReturn(2L);
            when(roleService.getRoleId("clubAdmin")).thenReturn(2L);
            when(activityService.findById(1L)).thenReturn(dbActivity);

            BusinessException exception = assertThrows(BusinessException.class, () -> {
                activityController.update(updateReq);
            });

            assertEquals("非草稿和已驳回状态的活动不能更新", exception.getMessage());
            verify(activityService, never()).updatedetail(any(), anyLong());
        }
    }

    // ==========================================
    // 测试 4：待审核 -> 审核通过 (状态流转 + 发送消息)
    // ==========================================
    @Test
    void testAudit_PendingStatus_ShouldPublishAndSendMessage() throws Exception {
        Long activityId = 1L;
        Activity dbActivity = new Activity();
        dbActivity.setId(activityId);
        dbActivity.setStatus("待审核"); // 🚨 当前待审核
        dbActivity.setCreatedBy(100L);
        dbActivity.setTitle("测试活动");

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getRoleId).thenReturn(1L); // 系统管理员
            when(roleService.getRoleId("admin")).thenReturn(1L);

            when(activityService.findById(activityId)).thenReturn(dbActivity);
            // 模拟更新状态成功
            when(activityService.updateStatus("已发布", activityId)).thenReturn(1);

            // 模拟消息写入成功
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"mock\":\"msg\"}");
            when(outboxMessageService.add(anyString(), anyString(), anyString())).thenReturn(1);

            // 执行
            Result result = activityController.audit(activityId);

            // 验证
            assertNotNull(result);
            // 1. 验证状态被改为了已发布
            verify(activityService).updateStatus("已发布", activityId);
            // 2. 验证确实写了 Outbox 消息
            verify(outboxMessageService).add(anyString(), eq("活动通过审核"), anyString());
        }
    }
}
