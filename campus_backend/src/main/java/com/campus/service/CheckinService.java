package com.campus.service;

import com.campus.entitys.Checkin;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface CheckinService {
    //添加签到记录
    int add(Long userId, Long activityId, Long registrationId);
    //获取所有签到记录
    List<Checkin> list();
    //更新签到状态
    void updateStatus(Long userId, Long activityId);
    //删除签到记录
    int delete(Long userId, Long activityId);
    //根据报名ID查询签到记录
    Checkin findByRegistrationId(Long id);
    //获取签到统计
    int count(Long activityId);
}
