package com.campus.service.impl;

import com.campus.entitys.Checkin;
import com.campus.mapper.CheckinMapper;
import com.campus.service.CheckinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CheckinServiceImpl implements CheckinService {
    @Autowired
    private CheckinMapper checkinMapper;
    @Override
    public int add(Long userId, Long activityId, Long registrationId) {
        return checkinMapper.add(userId,activityId,registrationId);
    }
    @Override
    public List<Checkin> list() {
        return checkinMapper.list();
    }
    @Override
    public void updateStatus(Long userId, Long activityId) {
        checkinMapper.updateStatus(userId,activityId);
    }
    @Override
    public int delete(Long userId, Long activityId) {
        return checkinMapper.delete(userId,activityId);
    }
    @Override
    public Checkin findByRegistrationId(Long registrationId) {
        return checkinMapper.findByRegistrationId(registrationId);
    }
    @Override
    public int count(Long activityId) {
        return checkinMapper.count(activityId);
    }
}
