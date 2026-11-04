package com.campus.service.impl;

import com.campus.entitys.Club;
import com.campus.mapper.ClubMapper;
import com.campus.service.ClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClubServiceImpl implements ClubService {
    @Autowired
    private ClubMapper clubMapper;

    @Override
    public void add(String clubName) {
        clubMapper.add(clubName);
    }

    @Override
    public void update(Club club,Long currentUserId) {
        clubMapper.update(club,currentUserId);
    }

    @Override
    public void disable(String clubName) {
        clubMapper.disable(clubName);
    }

    @Override
    public void enable(String clubName) {
        clubMapper.enable(clubName);
    }

    @Override
    public List<Club> list() {
        return clubMapper.list();
    }

    @Override
    public Club findByName(String clubName) {
        return clubMapper.findByName(clubName);
    }

    @Override
    public void assignAdmin(String clubName, Long userId) {
        clubMapper.assignAdmin(clubName, userId);
    }

    @Override
    public void deleteAdmin(String clubName) {
        clubMapper.deleteAdmin(clubName);
    }

    @Override
    public Club detail(String clubName) {
        return clubMapper.detail(clubName);
    }

    @Override
    public int addBookingNumbers(Long clubId) {
        return clubMapper.addBookingNumbers(clubId);
    }

    @Override
    public void reduceBookingNumbers(Long clubId) {
        clubMapper.reduceBookingNumbers(clubId);
    }

    @Override
    public int getBookingNumbers(Long clubId) {
        return clubMapper.getBookingNumbers(clubId);
    }
}
