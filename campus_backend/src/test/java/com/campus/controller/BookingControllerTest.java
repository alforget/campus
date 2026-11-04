package com.campus.controller;

import com.campus.entitys.*;
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
import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {
    @Mock
    private BookingService bookingService;
    @Mock private UserService userService;
    @Mock private VenueSlotService venueSlotService;
    @Mock private RoleService roleService;
    @Mock private ClubService clubService;
    @Mock private OutboxMessageService outboxMessageService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private BookingController bookingController;

    // 公共的模拟用户数据
    private User createMockUser() {
        User user = new User();
        user.setId(1001L);
        user.setClubId(50L);
        user.setRoleId(2L); // 社团管理员
        return user;
    }

    // ==========================================
    // 测试 1：社团最多 3 个有效预约 (提交被拦截)
    // ==========================================
    @Test
    void testSubmit_ClubHas3Bookings_ShouldReject() {
        Long bookingId = 1L;
        User user = createMockUser();

        Booking mockBooking = new Booking();
        mockBooking.setId(bookingId);
        mockBooking.setStatus("草稿");

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserName).thenReturn("testUser");
            when(userService.fingByName("testUser")).thenReturn(user);
            when(roleService.getRoleId("clubAdmin")).thenReturn(2L);

            when(bookingService.findById(bookingId)).thenReturn(mockBooking);
            // 🚨 核心：模拟该社团已经有 3 个有效预约
            when(clubService.getBookingNumbers(user.getClubId())).thenReturn(3);

            // 执行并断言异常
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                bookingController.submit(bookingId);
            });

            assertEquals("该社团待开始预约满3个，无法提交", exception.getMessage());

            // 验证：绝对没有去抢占时段
            verify(venueSlotService, never()).updateStatusToBOOKED(anyLong());
            verify(bookingService, never()).submit(anyLong(), anyInt(), anyLong());
        }
    }

    // ==========================================
    // 测试 2：同一时段只能成功一个 (并发抢占失败)
    // ==========================================
    @Test
    void testSubmit_SlotAlreadyBooked_ShouldReject() {
        Long bookingId = 1L;
        User user = createMockUser();

        Booking mockBooking = new Booking();
        mockBooking.setId(bookingId);
        mockBooking.setStatus("草稿");
        mockBooking.setSlotId(100L);

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserName).thenReturn("testUser");
            when(userService.fingByName("testUser")).thenReturn(user);
            when(roleService.getRoleId("clubAdmin")).thenReturn(2L);

            when(bookingService.findById(bookingId)).thenReturn(mockBooking);
            when(clubService.getBookingNumbers(user.getClubId())).thenReturn(0); // 没满3个

            // 🚨 核心：模拟抢占时段失败（返回 0，说明被别人抢了）
            when(venueSlotService.updateStatusToBOOKED(100L)).thenReturn(0);

            // 执行并断言
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                bookingController.submit(bookingId);
            });

            assertEquals("预约时段信息发生变化，请刷新查看后重试", exception.getMessage());

            // 验证：绝对没有去更新预约表
            verify(bookingService, never()).submit(anyLong(), anyInt(), anyLong());
        }
    }

    // ==========================================
    // 测试 3：24 小时内不能取消
    // ==========================================
    @Test
    void testCancel_Within24Hours_ShouldReject() {
        Long bookingId = 1L;
        User user = createMockUser();

        Booking mockBooking = new Booking();
        mockBooking.setId(bookingId);
        mockBooking.setStatus("已通过");
        mockBooking.setSlotId(100L);

        // 🚨 构造一个在 10 小时后开始的时段
        VenueSlot mockSlot = new VenueSlot();
        mockSlot.setSlotDate(LocalDate.now()); // 今天
        mockSlot.setStartTime(LocalTime.now().plusHours(10)); // 10小时后

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserName).thenReturn("testUser");
            when(userService.fingByName("testUser")).thenReturn(user);
            when(roleService.getRoleId("clubAdmin")).thenReturn(2L);

            when(bookingService.findById(bookingId)).thenReturn(mockBooking);
            when(venueSlotService.findById(100L)).thenReturn(mockSlot);

            // 执行并断言
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                bookingController.cancel(bookingId, "不想去了");
            });

            assertEquals("预约时间已到临界时间，24小时内不能取消预约,请联系场地管理员取消", exception.getMessage());

            // 验证：绝对没有去改数据库和释放时段
            verify(bookingService, never()).updateCancelReason(anyLong(), anyString(), anyInt(), anyLong());
            verify(venueSlotService, never()).updateStatus(anyLong(), eq("OPEN"));
            verify(clubService, never()).reduceBookingNumbers(anyLong());
        }
    }

    // ==========================================
    // 测试 4：正常取消 (超过 24 小时)
    // ==========================================
    @Test
    void testCancel_MoreThan24Hours_ShouldSuccess() throws Exception {
        Long bookingId = 1L;
        User user = createMockUser();

        Booking mockBooking = new Booking();
        mockBooking.setId(bookingId);
        mockBooking.setStatus("已通过");
        mockBooking.setSlotId(100L);
        mockBooking.setVersion(1);

        // 🚨 构造一个在 48 小时后开始的时段（超过24小时限制）
        VenueSlot mockSlot = new VenueSlot();
        mockSlot.setSlotDate(LocalDate.now().plusDays(2));
        mockSlot.setStartTime(LocalTime.now());

        try (MockedStatic<CurrentUserInfoUtil> mockedStatic = mockStatic(CurrentUserInfoUtil.class)) {
            mockedStatic.when(CurrentUserInfoUtil::getUserName).thenReturn("testUser");
            when(userService.fingByName("testUser")).thenReturn(user);
            when(roleService.getRoleId("clubAdmin")).thenReturn(2L);

            when(bookingService.findById(bookingId)).thenReturn(mockBooking);
            when(venueSlotService.findById(100L)).thenReturn(mockSlot);

            // 模拟更新成功
            when(bookingService.updateCancelReason(eq(bookingId), eq("不想去了"), eq(1), eq(1001L))).thenReturn(1);
            when(venueSlotService.updateStatus(100L, "OPEN")).thenReturn(1);

            // 执行
            Result result = bookingController.cancel(bookingId, "不想去了");

            // 断言
            assertNotNull(result);

            // 验证轨迹
            verify(bookingService).updateCancelReason(bookingId, "不想去了", 1, 1001L);
            verify(venueSlotService).updateStatus(100L, "OPEN");
            verify(clubService).reduceBookingNumbers(user.getClubId());
        }
    }
}
