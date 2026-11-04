package com.campus.controller;

import com.campus.entitys.Notification;
import com.campus.entitys.PageBean;
import com.campus.entitys.Result;
import com.campus.service.NotificationService;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/notification")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    //获取通知列表
    @GetMapping("/list")
    public Result<PageBean<Notification>> list(Integer pageNum, Integer pageSize, Long userId, @RequestParam(required = false) Integer readStatus) {
        return Result.success(notificationService.list(pageNum, pageSize,userId,readStatus));
    }

    // 标记通知为已读
    @GetMapping("/read")
    public Result<Notification> read(@RequestParam Long id) {
        notificationService.read(id);
        return Result.success();
    }

    //全部标记为已读
    @GetMapping("/readAll")
    public Result<Notification> readAll(@RequestParam Long userId) {
        notificationService.readAll(userId);
        return Result.success();
    }
}
