package com.campus.controller;

import com.campus.entitys.*;
import com.campus.exception.BusinessException;
import com.campus.exception.ErrorCode;
import com.campus.service.*;
import com.campus.utils.CurrentUserInfoUtil;
import com.campus.utils.ThreadLocalUtil;
import com.campus.vo.BookingVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/booking")
public class BookingController {
    @Autowired
    private BookingService bookingService;
    @Autowired
    private UserService userService;
    @Autowired
    private VenueSlotService venueSlotService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private ClubService clubService;
    @Autowired
    private OutboxMessageService outboxMessageService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private VenueService venueService;


    //添加预约 （仅社团管理员可操作）
    @PostMapping("/add")
    public Result add(Booking booking) {
        String username = CurrentUserInfoUtil.getUserName();
        User u = userService.fingByName(username);
        //判断是否是社团管理员
        Long roleIdFromDB = roleService.getRoleId("clubAdmin");
        if (!u.getRoleId().equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        //判断时段是否可以预约
        VenueSlot slot = venueSlotService.findById(booking.getSlotId());
        if (!slot.getStatus().equals("OPEN")) {
            throw new BusinessException("该时段已被预约");
        }
        //设置用户ID
        booking.setUserId(u.getId());
        //设置社团ID
        booking.setClubId(u.getClubId());
        try {
            bookingService.add(booking);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BusinessException("您已经预约过该时段，请勿重复提交");
        }
        return Result.success();
    }

    //编辑预约 （仅社团管理员可操作）
    @PutMapping("/update")
    public Result update(Booking booking) {
        String username = CurrentUserInfoUtil.getUserName();
        User u = userService.fingByName(username);
        //判断是否是社团管理员
        Long roleIdFromDB = roleService.getRoleId("clubAdmin");
        if (!u.getRoleId().equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        //设置用户ID
        booking.setUserId(u.getId());
        Booking existingBooking = bookingService.findById(booking.getId());
        if (existingBooking == null) {
            throw new BusinessException("预约不存在");
        }
        // 检查预约状态是否为草稿
        if (!"草稿".equals(existingBooking.getStatus())) {
            throw new BusinessException("预约状态错误，无法修改");
        }
        Integer version = existingBooking.getVersion();
        booking.setVersion(version);
        // 更新预约
        int rows = bookingService.update(booking);
        if(rows==0){
            throw new BusinessException("预约信息发生变化，请刷新后重试");
        }
        return Result.success();


    }

    //提交预约 （仅社团管理员可操作）
    @Transactional
    @PutMapping("/submit")
    public Result submit(Long id) {

        String username = CurrentUserInfoUtil.getUserName();
        User u = userService.fingByName(username);
        //判断是否是社团管理员
        Long roleIdFromDB = roleService.getRoleId("clubAdmin");
        if (!u.getRoleId().equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Booking existingBooking = bookingService.findById(id);
        if (existingBooking == null) {
            throw new BusinessException("预约不存在");
        }
        //判断社团待开始预约数量是否超过3个
        if (clubService.getBookingNumbers(u.getClubId()) >= 3) {
            throw new BusinessException("该社团待开始预约满3个，无法提交");
        }
        // 检查预约状态是否为草稿
        if (!"草稿".equals(existingBooking.getStatus())) {
            throw new BusinessException("预约状态错误，无法提交");
        }
        int rows1 = venueSlotService.updateStatusToBOOKED(existingBooking.getSlotId());
        if(rows1==0){
            throw new BusinessException("预约时段信息发生变化，请刷新查看后重试");
        }
        Integer version = existingBooking.getVersion();
        // 提交预约
        int rows = bookingService.submit(id, version, u.getId());
        if(rows==0){
            throw new RuntimeException("预约状态更新失败，请重试");
        }
        // 构造消息体 (JSON 字符串)
        String messageId = UUID.randomUUID().toString();
        String messageType = "预约成功";
        String title = "预约成功";
        String content = "恭喜你预约场地成功" ;
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("userId", u.getId());
        msgMap.put("messageId", messageId);
        msgMap.put("type", messageType);
        msgMap.put("title", title);
        msgMap.put("content", content);
        String payload = "";
        try {
            // 把 Map 转换成安全的 JSON 字符串
            payload = objectMapper.writeValueAsString(msgMap);
        } catch (Exception e) { throw new RuntimeException(e); }
        // 将消息写入出箱队列，等待异步处理
        int rows2 = outboxMessageService.add(messageId, messageType, payload);
        if (rows2 == 0) {
            throw new BusinessException("消息写入出箱队列失败");
        }
        // 增加社团预约数量
        clubService.addBookingNumbers(u.getClubId());
        return Result.success();
    }

/*
    //驳回预约 （仅场地管理员可操作）自动通过不需要这一步
    @PutMapping("/reject")
    public Result reject(Long id, String rejectReason) {

        String username = CurrentUserInfoUtil.getUserName();
        User u = userService.fingByName(username);
        //判断是否是场地管理员
        Long roleIdFromDB = roleService.getRoleId("venueAdmin");
        if (!u.getRoleId().equals(roleIdFromDB)) {
            return Result.error("您不是场地管理员，不能驳回预约");
        }
        Booking existingBooking = bookingService.findById(id);
        if (existingBooking == null) {
            return Result.error("预约不存在");
        }
        // 检查预约状态是否为待审核
        if (!"待审核".equals(existingBooking.getStatus())) {
            return Result.error("预约状态错误，无法驳回");
        }
        Integer version = existingBooking.getVersion();
        // 驳回预约
        int rows = bookingService.updateRejectReason(id, rejectReason, version);
        if(rows==0){
            return Result.error("预约信息发生变化，请刷新后重试");
        }
        return Result.success();
    }*/

    /*//通过预约 （仅场地管理员可操作） 改成自动通过
    @Transactional
    @PutMapping("/approve")
    public Result approve(Long id) {

        String username = CurrentUserInfoUtil.getUserName();
        User u = userService.fingByName(username);
        //判断是否是场地管理员
        Long roleIdFromDB = roleService.getRoleId("venueAdmin");
        if (!u.getRoleId().equals(roleIdFromDB)) {
            return Result.error("您不是场地管理员，不能通过预约");
        }
        Booking existingBooking = bookingService.findById(id);
        if (existingBooking == null) {
            return Result.error("预约不存在");
        }
        // 检查预约状态是否为待审核
        if (!"待审核".equals(existingBooking.getStatus())) {
            return Result.error("预约状态错误，无法通过");
        }
        Integer version = existingBooking.getVersion();
        // 通过预约
        bookingService.updateApprove(id, version);
        // 更新场地时段状态为已预约
        venueSlotService.updateStatus(existingBooking.getSlotId(), "BOOKED");
        return Result.success();
    }*/

    //取消预约 （社团管理员）
    @Transactional
    @PutMapping("/cancel")
    public Result cancel(Long id, String cancelReason) {

        String username = CurrentUserInfoUtil.getUserName();
        User u = userService.fingByName(username);
        //判断是否是社团管理员
        Long roleIdFromDB = roleService.getRoleId("clubAdmin");
        if (!u.getRoleId().equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Booking existingBooking = bookingService.findById(id);
        if (existingBooking == null) {
            throw new BusinessException("预约不存在");
        }
        // 检查预约状态是否为已通过
        if (!"已通过".equals(existingBooking.getStatus())) {
            throw new BusinessException("预约状态错误，无法取消");
        }
        //判断预约时间是否已到临界时间，24小时内不能取消预约
        //预约的时段
        VenueSlot slot = venueSlotService.findById(existingBooking.getSlotId());
        //预约起始时间
        LocalDateTime startTime = slot.getSlotDate().atTime(slot.getStartTime());
        if (LocalDateTime.now().isAfter(startTime.minusDays(1))) {
            throw new BusinessException("预约时间已到临界时间，24小时内不能取消预约,请联系场地管理员取消");
        }
        Integer version = existingBooking.getVersion();
        // 取消预约
        int rows = bookingService.updateCancelReason(id, cancelReason, version, u.getId());
        if(rows==0){
            throw new BusinessException("预约信息发生变化，请刷新后重试");
        }
        // 更新场地时段状态为开放
        int rows2 = venueSlotService.updateStatus(existingBooking.getSlotId(), "OPEN");
        if(rows2==0){
            throw new BusinessException("场地时段状态发生变化，请刷新后重试");
        }
        // 减少社团预约数量
        clubService.reduceBookingNumbers(u.getClubId());
        return Result.success();
    }

    //取消预约 （场地管理员）
    @Transactional
    @PutMapping("/cancelByVenueAdmin")
    public Result cancelByVenueAdmin(Long id, String cancelReason) {

        String username = CurrentUserInfoUtil.getUserName();
        User u = userService.fingByName(username);
        //判断是否是场地管理员
        Long roleIdFromDB = roleService.getRoleId("venueAdmin");
        if (!u.getRoleId().equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Booking existingBooking = bookingService.findById(id);
        if (existingBooking == null) {
            throw new BusinessException("预约不存在");
        }
        // 检查预约状态是否为已通过
        if (!"已通过".equals(existingBooking.getStatus())) {
            throw new BusinessException("预约状态错误，无法取消");
        }
        Integer version = existingBooking.getVersion();
        // 取消预约
        int rows = bookingService.updateCancelReason2(id, cancelReason, version, u.getId());
        if(rows==0){
            throw new BusinessException("预约信息发生变化，请刷新后重试");
        }
        // 更新场地时段状态为开放
        int rows2 = venueSlotService.updateStatus(existingBooking.getSlotId(), "OPEN");
        if(rows2==0){
            throw new BusinessException("场地时段状态发生变化，请刷新后重试");
        }
        // 构造消息体 (JSON 字符串)
        String messageId = UUID.randomUUID().toString();
        String messageType = "预约取消";
        String title = "预约取消";
        String content = "你的场地预约已被场地管理员取消，取消原因：" + cancelReason;
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("userId", existingBooking.getUserId());
        msgMap.put("messageId", messageId);
        msgMap.put("type", messageType);
        msgMap.put("title", title);
        msgMap.put("content", content);
        String payload = "";
        try {
            // 把 Map 转换成安全的 JSON 字符串
            payload = objectMapper.writeValueAsString(msgMap);
        } catch (Exception e) { throw new RuntimeException(e); }
        // 将消息写入出箱队列，等待异步处理
        int rows3 = outboxMessageService.add(messageId, messageType, payload);
        if (rows3 == 0) {
            throw new BusinessException("消息写入出箱队列失败");
        }
        // 减少社团预约数量
        clubService.reduceBookingNumbers(existingBooking.getClubId());
        return Result.success();
    }

   //我社团的预约 （仅社团管理员可操作）
    @GetMapping("/listForClubAdmin")
    public Result<PageBean<BookingVO>> listForClubAdmin(Integer pageNum, Integer pageSize,
                                                      @RequestParam(required = false) String status
                                                      ) {
        String username = CurrentUserInfoUtil.getUserName();
        User u = userService.fingByName(username);
        //判断是否是社团管理员
        Long roleIdFromDB = roleService.getRoleId("clubAdmin");
        if (!u.getRoleId().equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        PageBean<Booking> bookings = bookingService.listForClubAdmin(pageNum, pageSize, status, u.getClubId());
        return Result.success(toVoPage(bookings));
    }

    //全部预约列表 （仅场地管理员可操作）
    @GetMapping("/list")
    public Result<PageBean<BookingVO>> list(Integer pageNum, Integer pageSize,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) Long clubId
                                                      ) {
        String username = CurrentUserInfoUtil.getUserName();
        User u = userService.fingByName(username);
        //判断是否是场地管理员
        Long roleIdFromDB = roleService.getRoleId("venueAdmin");
        if (!u.getRoleId().equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        PageBean<Booking> bookings = bookingService.list(pageNum, pageSize, status, clubId);
        return Result.success(toVoPage(bookings));
    }

    /**
     * 预约实体转 VO，补齐场馆、时段、社团、用户展示字段。
     */
    private PageBean<BookingVO> toVoPage(PageBean<Booking> page) {
        Map<Long, String> clubNames = clubService.list().stream()
                .collect(Collectors.toMap(Club::getId, Club::getName, (a, b) -> a));
        List<BookingVO> items = page.getItems().stream().map(booking -> {
            VenueSlot slot = venueSlotService.findById(booking.getSlotId());
            Venue venue = slot == null ? null : venueService.detail(slot.getVenueId());
            User user = userService.findById(booking.getUserId());
            return BookingVO.from(
                    booking,
                    slot == null ? null : slot.getVenueId(),
                    venue == null ? null : venue.getName(),
                    venue == null ? null : venue.getLocation(),
                    slot == null ? null : slot.getSlotDate(),
                    slot == null ? null : slot.getStartTime(),
                    slot == null ? null : slot.getEndTime(),
                    user == null ? null : user.getUsername(),
                    clubNames.get(booking.getClubId())
            );
        }).collect(Collectors.toList());
        return new PageBean<>(page.getTotal(), items);
    }
}