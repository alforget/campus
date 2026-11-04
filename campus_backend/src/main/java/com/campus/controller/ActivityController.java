package com.campus.controller;

import com.campus.entitys.*;
import com.campus.exception.BusinessException;
import com.campus.exception.ErrorCode;
import com.campus.mapper.OutboxMessageMapper;
import com.campus.mapper.RegistrationMapper;
import com.campus.service.*;
import com.campus.utils.CurrentUserInfoUtil;
import com.campus.vo.ActivityVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/activity")
public class ActivityController {
    @Autowired
    private ActivityService activityService;
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private OutboxMessageService outboxMessageService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private VenueSlotService venueSlotService;
    @Autowired
    private ClubService clubService;
    @Autowired
    private VenueService venueService;
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;
    @Autowired
    private RegistrationService registrationService;

    //添加活动 仅社团管理员可以添加活动
    @PostMapping("/add")
    public Result add(@RequestBody @Validated(Activity.Add.class) Activity activity) {
        //判断是否为社团管理员
        Long roleId = CurrentUserInfoUtil.getRoleId();
        Long roleIdFromDB = roleService.getRoleId("clubAdmin");
        if (!roleId.equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        //判断场地时段是否存在
        LocalDate slotDate = activity.getStartTime().toLocalDate();
        LocalTime startTime = activity.getStartTime().toLocalTime();
        LocalTime endTime = activity.getEndTime().toLocalTime();
        List<VenueSlot> slots = venueSlotService.findByVenueIdAndTime(activity.getVenueId(),slotDate,startTime,endTime);
        if (slots.isEmpty()) {
            throw new BusinessException("场地时段不存在");
        }
        //判断场地时段是否存在在预约表中且状态为BOOKED
        for (VenueSlot slot : slots) {
            Booking booking = bookingService.findByVenueSlotIdAndStatus(slot.getId(),"BOOKED");
            if(booking==null){
                continue;
            }
            if(!booking.getClubId().equals(activity.getClubId())){
                throw new BusinessException("你的活动时间与预约时间冲突");
            }
        }

        if (activity.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("活动开始时间不能早于当前时间");
        }
        if (activity.getSignupStart().isBefore(LocalDateTime.now())) {
            throw new BusinessException("活动报名开始时间不能早于当前时间");
        }
        if (activity.getSignupStart().isAfter(activity.getSignupEnd())) {
            throw new BusinessException("报名开始时间不能晚于报名结束时间");
        }
        if (activity.getStartTime().isBefore(activity.getSignupEnd())) {
            throw new BusinessException("活动开始时间不能早于活动报名结束时间");
        }
        if (activity.getStartTime().isAfter(activity.getEndTime())) {
            throw new BusinessException("活动开始时间不能晚于活动结束时间");
        }
        Long userId = CurrentUserInfoUtil.getUserId();
        User user = userService.findById(userId);
        activity.setClubId(user.getClubId());
        activity.setCreatedBy(userId);
        activity.setRemaining(activity.getCapacity());
        activity.setWaitlistRemaining(activity.getWaitlistLimit());
        activityService.add(activity);
        return Result.success();
    }

    //获得活动列表 (管理员端)
    @GetMapping("/list")
    public Result<PageBean<ActivityVO>> list(
            Integer pageNum, Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) Boolean isArchive
    ) {
        //判断是否为管理员
        Long roleId = CurrentUserInfoUtil.getRoleId();
        Long roleIdFromDB = roleService.getRoleId("admin");
        if (!roleId.equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return Result.success(toVoPage(activityService.list(pageNum, pageSize, status, clubId, isArchive)));
    }

    //获得活动列表 (学生端)
    @GetMapping("/listforstudent")
    public Result<PageBean<ActivityVO>> listForStudent(@RequestParam Integer pageNum, @RequestParam Integer pageSize) {
        return Result.success(toVoPage(activityService.listForStudent(pageNum, pageSize)));
    }

    //获得活动列表 (社团管理员端)
    @GetMapping("/listforclub")
    public Result<PageBean<ActivityVO>> listForClub(Integer pageNum, Integer pageSize, @RequestParam(required = false) String status, @RequestParam(required = false) Boolean isArchive) {
        //判断是否为社团管理员
        Long roleId = CurrentUserInfoUtil.getRoleId();
        Long roleIdFromDB = roleService.getRoleId("clubAdmin");
        if (!roleId.equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        String username = CurrentUserInfoUtil.getUserName();
        Long clubId = userService.fingByName(username).getClubId();
        return Result.success(toVoPage(activityService.listForClub(pageNum, pageSize, status, isArchive, clubId)));
    }

    //获得活动详情
    @GetMapping("/detail")
    public Result<ActivityVO> detail(Long id) {
        Activity activity = null;
        Long roleId = CurrentUserInfoUtil.getRoleId();
        //判断是否为管理员
        Long roleIdFromDB = roleService.getRoleId("admin");
        if (roleId.equals(roleIdFromDB)) {
            activity = activityService.findById(id);
        }
        //判断是否是社团管理员
        Long clubId = userService.fingByName(CurrentUserInfoUtil.getUserName()).getClubId();
        roleIdFromDB = roleService.getRoleId("clubAdmin");
        if (roleId.equals(roleIdFromDB)) {
            activity = activityService.findByIdForClubAdmin(id,clubId);
        }
        //判断是否是学生
        roleIdFromDB = roleService.getRoleId("student");
        if (roleId.equals(roleIdFromDB)) {
            activity = activityService.findByIdForStudent(id);
        }
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        String clubName = clubService.list().stream()
                .filter(c -> c.getId().equals(clubId))
                .map(Club::getName).findFirst().orElse(null);
        Venue venue = activity.getVenueId() == null ? null : venueService.detail(activity.getVenueId());
        return Result.success(ActivityVO.from(activity, clubName, venue == null ? null : venue.getName()));
    }

    //更新活动 仅社团管理员可以更新
    @PutMapping("/update")
    public Result update(@RequestBody @Validated(Activity.Update.class) Activity activity) {
            //判断是否为社团管理员
            Long userId = CurrentUserInfoUtil.getUserId();
            Long roleId = CurrentUserInfoUtil.getRoleId();
            Long roleIdFromDB = roleService.getRoleId("clubAdmin");
            if (!roleId.equals(roleIdFromDB)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            //判断活动状态是否为已开始
            Activity a = activityService.findById(activity.getId());
            if (!a.getStatus().equals("草稿") && !a.getStatus().equals("已驳回")) {
                throw new BusinessException("非草稿和已驳回状态的活动不能更新");
            }
            activity.setVersion(a.getVersion());
            int rows = activityService.updatedetail(activity, userId);
            if(rows == 0){
                throw new BusinessException("更新活动失败,请重新尝试");
            }
        return Result.success();
    }

    //提交活动 仅社团管理员可以提交
    @PatchMapping("/submit")
    public Result submit(Long id) {
            //判断是否为社团管理员
            Long userId = CurrentUserInfoUtil.getUserId();
            Long roleId = CurrentUserInfoUtil.getRoleId();
            Long roleIdFromDB = roleService.getRoleId("clubAdmin");
            if (!roleId.equals(roleIdFromDB)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            Activity a = activityService.findById(id);
            //判断活动状态是否为草稿或已驳回
            if (!a.getStatus().equals("草稿") && !a.getStatus().equals("已驳回")) {
                throw new BusinessException("非草稿和已驳回状态的活动不能提交");
            }
            int rows = activityService.submit(id, userId, "待审核");
            if(rows == 0){
                throw new BusinessException("提交活动失败,请重新尝试");
            }
        return Result.success();
    }

    //通过审核活动 仅管理员可以通过审核
    @PatchMapping("/audit")
    @Transactional
    public Result audit(Long id) {
        //判断是否为管理员
        Long roleId = CurrentUserInfoUtil.getRoleId();
        Long roleIdFromDB = roleService.getRoleId("admin");
        if (!roleId.equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Activity a = activityService.findById(id);
        if (!a.getStatus().equals("待审核")) {
            throw new BusinessException("非待审核状态的活动不能审核");
        }
        activityService.updateStatus("已发布", id);
        // 构造消息体 (JSON 字符串)
        String messageId = UUID.randomUUID().toString();
        String messageType = "活动通过审核";
        String title = "活动通过审核";
        String content = "恭喜你申请活动通过审核，活动名称：" + a.getTitle();
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("userId", a.getCreatedBy());
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
        return Result.success();
    }

    //驳回审核活动 仅管理员可以驳回审核
    @PutMapping("/reject")
    @Transactional
    public Result reject(Long id, @NotBlank(message = "驳回原因不能为空")
    @Size(min = 5, max = 200, message = "驳回原因需要5-200个字") String rejectReason) {
        //判断是否为管理员
        Long roleId = CurrentUserInfoUtil.getRoleId();
        Long roleIdFromDB = roleService.getRoleId("admin");
        if (!roleId.equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Activity a = activityService.findById(id);
        if (!a.getStatus().equals("待审核")) {
            throw new BusinessException("非待审核状态的活动不能驳回审核");
        }
        a.setStatus("已驳回");
        a.setRejectReason(rejectReason);
        activityService.reject(a);
        // 构造消息体 (JSON 字符串)
        String messageId = UUID.randomUUID().toString();
        String messageType = "活动被驳回";
        String title = "活动被驳回";
        String content = "很遗憾你申请活动被驳回，活动名称：" + a.getTitle();
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("userId", a.getCreatedBy());
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
        return Result.success();
    }

    //取消活动 仅社团管理员可以取消
    @PatchMapping("/cancel")
    @Transactional(rollbackFor = Exception.class) // 确保任何异常都回滚
    public Result cancel(Long id) {
        Activity a = activityService.findById(id);
        if (a == null) {
            throw new BusinessException("活动不存在");
        }

        // 1. 权限校验
        Long userId = CurrentUserInfoUtil.getUserId();
        User u = userService.findById(userId);
        Long roleId = CurrentUserInfoUtil.getRoleId();
        Long roleIdFromDB = roleService.getRoleId("clubAdmin");
        if (!roleId.equals(roleIdFromDB) || !a.getClubId().equals(u.getClubId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 2. 状态校验
        if ("草稿".equals(a.getStatus()) || "已结束".equals(a.getStatus()) ||
                "已取消".equals(a.getStatus()) || "已归档".equals(a.getStatus())) {
            throw new BusinessException("当前状态的活动不能取消");
        }

        // 3. 更新活动状态和余额
        activityService.updateStatus("已取消", id);
        int rows = activityService.updateRemaining(id, 0);
        if(rows == 0) {
            throw new BusinessException("更新活动余额失败");
        }

        // 4. 查出所有报名该活动的用户ID
        List<Long> userIds = registrationService.findAllByActivityId(id);
        if (userIds == null || userIds.isEmpty()) {
            return Result.success(); // 没人报名，直接结束
        }

        // 5. 批量更新报名状态为已取消 (走普通 Spring 事务)
        registrationService.batchCancelRegistrations(id, userIds);

        // 6. 批量构造 Outbox 消息
        // 因为是同一个活动，复用外层的 a.getTitle()，不要循环查库
        String title = "报名的活动被取消";
        String content = "你报名的活动已被取消了，活动名称：" + a.getTitle();

        for (Long targetUserId : userIds) {
            String messageId = UUID.randomUUID().toString();
            String messageType = "报名的活动被取消";

            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("userId", targetUserId);
            msgMap.put("messageId", messageId);
            msgMap.put("type", messageType);
            msgMap.put("title", title);
            msgMap.put("content", content);

            String payload;
            try {
                payload = objectMapper.writeValueAsString(msgMap);
            } catch (Exception e) {
                throw new RuntimeException("消息序列化失败", e);
            }

            // 直接调普通的 Mapper 插入，在同一个 @Transactional 事务里保证原子性
            outboxMessageService.add(messageId, messageType, payload);
        }

        return Result.success();
    }

    //删除活动 仅管理员可以删除
    @DeleteMapping("/delete")
    public Result delete(Long id) {
        //判断是否为管理员
        Long roleId = CurrentUserInfoUtil.getRoleId();
        Long roleIdFromDB = roleService.getRoleId("admin");
        if (!roleId.equals(roleIdFromDB)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Activity a = activityService.findById(id);
        if (!a.getStatus().equals("草稿") && !a.getStatus().equals("已驳回")) {
            throw new BusinessException("非草稿和已驳回状态的活动不能删除");
        }
        activityService.delete(id);
        return Result.success();
    }

    //删除活动 社团管理员可以删除
    @DeleteMapping("/deleteFromClubAdmin")
    public Result deleteFromClubAdmin(Long id) {
        //判断是否为社团管理员
        Long userId = CurrentUserInfoUtil.getUserId();
        User u = userService.findById(userId);
        Long roleId = CurrentUserInfoUtil.getRoleId();
        Long roleIdFromDB = roleService.getRoleId("clubAdmin");
        if (!roleId.equals(roleIdFromDB) || !u.getClubId().equals(activityService.findById(id).getClubId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Activity a = activityService.findById(id);
        if (!a.getStatus().equals("草稿") && !a.getStatus().equals("已驳回")) {
            throw new BusinessException("非草稿和已驳回状态的活动不能删除");
        }
        activityService.delete(id);
        return Result.success();
    }

    /**
     * 活动实体转 VO，并补齐社团名、场馆名。
     */
    private PageBean<ActivityVO> toVoPage(PageBean<Activity> page) {
        Map<Long, String> clubNames = clubService.list().stream()
                .collect(Collectors.toMap(Club::getId, Club::getName, (a, b) -> a));
        Map<Long, String> venueNames = venueService.list().stream()
                .collect(Collectors.toMap(Venue::getId, Venue::getName, (a, b) -> a));
        List<ActivityVO> items = page.getItems().stream()
                .map(a -> ActivityVO.from(a, clubNames.get(a.getClubId()), venueNames.get(a.getVenueId())))
                .collect(Collectors.toList());
        return new PageBean<>(page.getTotal(), items);
    }
}
