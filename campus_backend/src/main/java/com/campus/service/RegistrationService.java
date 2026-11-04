package com.campus.service;

import com.campus.entitys.PageBean;
import com.campus.entitys.Registration;
import com.campus.entitys.Result;

import java.util.List;

public interface RegistrationService {
    //添加报名
    void add(Registration registration);

    //更新报名状态
    int updateStatus(Long id, String status);

    //根据报名ID查询报名详情
    Registration findById(Long id);

    //删除报名
    int delete(Long id);

    //根据活动ID或状态获取报名列表
    PageBean<Registration> list(Integer pageNum, Integer pageSize, Long activityId, String status);

    //根据活动ID删除所有报名
    void deleteActivity(Long id);

    //根据用户ID和活动ID查询报名详情
    Registration findByUserIdAndActivityId(Long userId, Long activityId);

    //查询已结束活动的报名
    List<Registration> selectArchive();

    //查询已结束但未归档的活动且未签到的报名
    List<Registration> selectEnd();

    //根据活动ID和状态查询报名列表
    List<Registration> findByActivityIdAndStatus(Long activityId, String status);

    //取消报名
    void cancel(Long registrationId, Long userId, String cancelReason);

    //根据用户ID查询报名列表（学生）
    PageBean<Registration> listForStudent(Integer pageNum, Integer pageSize, Long activityId, String status, Long userId);

    //全量查询报名列表
    List<Registration> listAll(Long activityId, String status);

    //报名功能，幂等键去重
    Result addWithIdempotencyKey(Long activityId, String idempotencyKey);

    //取消报名实现
    Result cancelImpl(Long registrationId, String cancelReason);

    //根据用户ID和票号查询报名详情
    Registration findByUserIdAndTicketNo(Long userId, String ticketNo);

    //获取报名总人数
    int count(Long activityId);

    //根据活动ID查询所有报名用户ID
    List<Long> findAllByActivityId(Long activityId);

    //补位方法
    void fillWaitlistToFull(Long activityId);
    //批量取消报名
    void batchCancelRegistrations(Long id, List<Long> userIds);
}
