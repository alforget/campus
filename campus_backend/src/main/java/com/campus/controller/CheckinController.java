package com.campus.controller;


import com.campus.entitys.*;
import com.campus.exception.BusinessException;
import com.campus.exception.ErrorCode;
import com.campus.service.*;
import com.campus.utils.CurrentUserInfoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/checkin")
public class CheckinController {
    @Autowired
    private CheckinService checkinService;

    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private ActivityService activityService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserService userService;

    /*//添加待签到记录
    @PostMapping("/add")
    public Result add(Long userId, Long activityId, Long registrationId) {
        checkinService.add(userId,activityId,registrationId);
        return Result.success();
    }*/
    //签到 （仅社团管理员可操作）
    @Transactional
    @PutMapping("/checkin")
    public Result checkin(String ticketNo) {
        //  角色校验：必须是社团管理员
        Long currentRoleId = CurrentUserInfoUtil.getRoleId();
        Long clubAdminRoleId = roleService.getRoleId("clubAdmin");
        if (!currentRoleId.equals(clubAdminRoleId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        //检验用户的票号
        //获取当前用户的签到记录
        Long userId = CurrentUserInfoUtil.getUserId();
        User currentUser = userService.findById(userId);
        Registration registration = registrationService.findByUserIdAndTicketNo(userId, ticketNo);
        if (registration == null) {
            throw new BusinessException("票号错误或未报名，无法签到");
        }
        Long activityId = registration.getActivityId();
        // 查询活动并校验归属
        Activity activity = activityService.findById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if (!activity.getClubId().equals(currentUser.getClubId())) {
            throw new BusinessException("您不是该活动的主办方，无权签到");
        }

        activity = activityService.findById(activityId);
        //校验现在是不是签到时间
        if (LocalDateTime.now().isBefore(activity.getStartTime().minusMinutes(30))) {
            throw new BusinessException("签到未开始，无法签到");
        }
        if (activity.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("签到已结束，无法签到");
        }
        //校验用户是否已签到
        Checkin checkin = checkinService.findByRegistrationId(registration.getId());
        if (checkin != null) {
            throw new BusinessException("您已签到，无法重复签到");
        }
        //添加签到记录
        int rows = checkinService.add(userId, activityId, registration.getId());
        if (rows == 0) {
            throw new BusinessException("签到失败,请稍后重试");
        }
        //更新报名状态
        int rows1 = registrationService.updateStatus(registration.getId(), "已签到");
        if (rows1 == 0) {
            throw new BusinessException("更新报名状态失败");
        }
        return Result.success();
    }

    //获取签到统计
    @GetMapping("/count")
    public Result<String> count(Long activityId) {
        //已签到人数
        int count = checkinService.count(activityId);
        //总人数
        int totalCount = registrationService.count(activityId);
        //未签到人数
        int uncount = totalCount - count;
        String data = "目前已签到人数："+count+" 目前未签到人数："+uncount+" 应签到总人数；"+totalCount;
        return Result.success(data);
    }
}
