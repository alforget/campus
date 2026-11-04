package com.campus.service;

import com.campus.entitys.Venue;

import java.util.List;

public interface VenueService {
    //获取全部数据
    List<Venue> list();
    //添加场馆 （仅场地管理员或管理员可操作）
    void add(Venue venue);
    //编辑场馆 （仅场地管理员或管理员可操作）
    void update(Venue venue);
    //删除场馆 （仅场地管理员或管理员可操作）
    void delete(Long id);
    //根据ID查询场馆详情
    Venue detail(Long id);
    //更新场馆状态
    void updateStatus(Long id, int status);
}
