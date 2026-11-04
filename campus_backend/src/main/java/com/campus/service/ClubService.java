package com.campus.service;

import com.campus.entitys.Club;

import java.util.List;

public interface ClubService {
    // 创建社团
    void add(String clubName);
    // 更新社团信息
    void update(Club club,Long currentUserId);
    // 禁用社团
    void disable(String clubName);
    // 启用社团
    void enable(String clubName);
    // 查询所有社团
    List<Club> list();
    // 根据名称查询社团
    Club findByName(String clubName);
    // 为社团分配管理员 (管理员权限)
    void assignAdmin(String clubName, Long userId);
    // 删除社团管理员 (管理员权限)
    void deleteAdmin(String clubName);
    // 查询社团详情
    Club detail(String clubName);
    // 增加社团预约数量
    int addBookingNumbers(Long clubId);
    // 减少社团预约数量
    void reduceBookingNumbers(Long clubId);
    // 查询社团预约数量
    int getBookingNumbers(Long clubId);
}
