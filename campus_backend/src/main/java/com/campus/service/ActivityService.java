package com.campus.service;

import com.campus.entitys.Activity;
import com.campus.entitys.PageBean;

import java.util.List;

public interface ActivityService {
    // 添加活动
    void add(Activity activity);
    //获得活动列表 (管理员端)
    PageBean<Activity> list(Integer pageNum, Integer pageSize, String status, Long clubId, Boolean isArchive);

    //获得活动详情
    Activity findById(Long id);
    //驳回活动
    int reject(Activity activity);
    //更新活动详情
    int updatedetail(Activity activity, Long userId);
    //获得活动列表（学生端）
    PageBean<Activity> listForStudent(Integer pageNum, Integer pageSize);
    //更新活动状态
    int updateStatus(String status,Long id);

    //报名活动
    int register(Long activityId);
    //更新活动容量
    int adjustRemaining(Long activityId);
    //查询应改为报名中的活动
    List<Activity> selectSignUp();
    //查询应改为已开始的活动
    List<Activity> selectStart();
    //查询应改为已结束的活动
    List<Activity> selectEnd();
    //查询应改为报名已结束的活动
    List<Activity> selectSignupEnd();
    //查询应改为已归档的活动

    List<Activity> selectArchive();
    //提交活动
    int submit(Long id, Long userId, String status);
    //获得活动列表 (社团管理员端)
    PageBean<Activity> listForClub(Integer pageNum, Integer pageSize, String status, Boolean isArchive, Long clubId);
    //删除活动 仅管理员可以删除
    void delete(Long id);
    //添加候补
    int addWaitList(Long activityId);
    //增加候补名额
    int adjustWaitList(Long activityId);
    //查询需要补位的活动ID
    List<Long> findActivitiesNeedCompensate();
    //根据活动ID和社团ID查询活动详情（社团管理员端）
    Activity findByIdForClubAdmin(Long id, Long clubId);
    //根据活动ID查询活动详情（学生端）
    Activity findByIdForStudent(Long id);
    //更新活动余额
    int updateRemaining(Long id, int i);
}
