package com.campus.service;

import com.campus.entitys.PageBean;
import com.campus.entitys.VenueSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface VenueSlotService {
    //添加场地时段（初始分配）
    void add(VenueSlot venueSlot);

    //删除指定日期的时段
    void deleteBySlotDate(LocalDate localDate);
    //根据ID查询场地时段
    VenueSlot findById(Long slotId);
    // 更新场地时段状态
    int updateStatus(Long slotId, String status);
    //根据场地ID和活动开始结束时间查询场地时段
    List<VenueSlot> findByVenueIdAndTime(Long venueId, LocalDate slotDate, LocalTime startTime, LocalTime endTime);
    //按日期和场馆ID查询时段列表
    PageBean<VenueSlot> list(Integer pageNum, Integer pageSize, Long id, LocalDate slotDate);
    // 更新场地时段状态为已预约
    int updateStatusToBOOKED(Long slotId);
}
