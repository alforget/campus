package com.campus.service.impl;

import com.campus.entitys.Activity;
import com.campus.entitys.PageBean;
import com.campus.mapper.ActivityMapper;
import com.campus.service.ActivityService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {
    @Autowired
    private ActivityMapper activityMapper;

    @Override
    public void add(Activity activity) {
        activityMapper.add(activity);
    }

    @Override
    public PageBean<Activity> list(Integer pageNum, Integer pageSize, String status, Long clubId, Boolean isArchive) {
        PageBean<Activity> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Activity> list = activityMapper.list(status, clubId, isArchive);
        Page page = (Page) list;
        pb.setTotal(page.getTotal());
        pb.setItems(page.getResult());
        return pb;
    }

    @Override
    public Activity findById(Long id) {
        return activityMapper.findById(id);
    }

    @Override
    public int reject(Activity activity) {
        return activityMapper.reject(activity);
    }

    @Override
    public int updatedetail(Activity activity, Long userId) {
        return activityMapper.updatedetail(activity, userId);
    }

    @Override
    public PageBean<Activity> listForStudent(Integer pageNum, Integer pageSize) {
        PageBean<Activity> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Activity> list = activityMapper.listForStudent();
        Page page = (Page) list;
        pb.setTotal(page.getTotal());
        pb.setItems(page.getResult());
        return pb;
    }


    @Override
    public int updateStatus(String status, Long id) {
        return activityMapper.updateStatus(status, id);
    }

    @Override
    public int register(Long activityId) {
        return activityMapper.register(activityId);
    }

    @Override
    public int adjustRemaining(Long activityId) {
        return activityMapper.adjustRemaining(activityId);
    }

    @Override
    public List<Activity> selectSignUp() {
        return activityMapper.selectSignUp();
    }

    @Override
    public List<Activity> selectStart() {
        return activityMapper.selectStart();
    }

    @Override
    public List<Activity> selectEnd() {
        return activityMapper.selectEnd();
    }

    @Override
    public List<Activity> selectSignupEnd() {
        return activityMapper.selectSignupEnd();
    }

    @Override
    public List<Activity> selectArchive() {
        return activityMapper.selectArchive();
    }

    @Override
    public int submit(Long id, Long userId, String status) {
        return activityMapper.submit(id, userId, status);
    }

    @Override
    public PageBean<Activity> listForClub(Integer pageNum, Integer pageSize, String status, Boolean isArchive, Long clubId) {
        PageBean<Activity> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Activity> list = activityMapper.listForClub(status, isArchive, clubId);
        Page page = (Page) list;
        pb.setTotal(page.getTotal());
        pb.setItems(page.getResult());
        return pb;
    }

    @Override
    public void delete(Long id) {
        activityMapper.delete(id);
    }

    @Override
    public int addWaitList(Long activityId) {
        return activityMapper.addWaitList(activityId);
    }

    @Override
    public int adjustWaitList(Long activityId) {
        return activityMapper.adjustWaitList(activityId);
    }

    @Override
    public List<Long> findActivitiesNeedCompensate() {
        return activityMapper.findActivitiesNeedCompensate();
    }

    @Override
    public Activity findByIdForClubAdmin(Long id, Long clubId) {
        return activityMapper.findByIdForClubAdmin(id, clubId);
    }

    @Override
    public Activity findByIdForStudent(Long id) {
        return activityMapper.findByIdForStudent(id);
    }

    @Override
    public int updateRemaining(Long id, int i) {
        return activityMapper.updateRemaining(id, i);
    }
}