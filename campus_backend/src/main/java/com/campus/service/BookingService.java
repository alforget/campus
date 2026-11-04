package com.campus.service;

import com.campus.entitys.Booking;
import com.campus.entitys.PageBean;

public interface BookingService {
    //添加预约
    void add(Booking booking);

    //根据ID查询预约
    Booking findById(Long id);

    //更新预约
    int update(Booking booking);

    //驳回预约
    int updateRejectReason(Long id, String rejectReason, Integer version);

    //通过预约
    int updateApprove(Long id, Integer version);

    //提交预约
    int submit(Long id, Integer version, Long userId);

    //取消预约
    int updateCancelReason(Long id, String cancelReason, Integer version, Long cancelledBy);

    //取消预约 (场地管理员)
    int updateCancelReason2(Long id, String cancelReason, Integer version, Long cancelledBy);
    //根据场地时段ID和状态查询预约
    Booking findByVenueSlotIdAndStatus(Long id, String status);
    //根据社团ID或状态查询预约 （仅社团管理员可操作）
    PageBean<Booking> listForClubAdmin(Integer pageNum, Integer pageSize, String status, Long clubId);
    //根据社团ID或状态查询预约 （仅场地管理员可操作）
    PageBean<Booking> list(Integer pageNum, Integer pageSize, String status, Long clubId);
}
