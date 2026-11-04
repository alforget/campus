package com.campus.service.impl;

import com.campus.entitys.Booking;
import com.campus.entitys.PageBean;
import com.campus.mapper.BookingMapper;
import com.campus.service.BookingService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {
    @Autowired
    private BookingMapper bookingMapper;

    @Override
    public void add(Booking booking) {
        bookingMapper.add(booking);
    }

    @Override
    public Booking findById(Long id) {
        return bookingMapper.findById(id);
    }

    @Override
    public int update(Booking booking) {
        return bookingMapper.update(booking);
    }

    @Override
    public int updateRejectReason(Long id, String rejectReason, Integer version) {
        return bookingMapper.updateRejectReason(id, rejectReason, version);
    }

    @Override
    public int updateApprove(Long id, Integer version) {
        return bookingMapper.updateApprove(id, version);
    }

    @Override
    public int submit(Long id, Integer version, Long userId) {
        return bookingMapper.submit(id, version, userId);
    }

    @Override
    public int updateCancelReason(Long id, String cancelReason, Integer version, Long cancelledBy) {
        return bookingMapper.updateCancelReason(id, cancelReason, version, cancelledBy);
    }

    @Override
    public int updateCancelReason2(Long id, String cancelReason, Integer version, Long cancelledBy) {
        return bookingMapper.updateCancelReason2(id, cancelReason, version, cancelledBy);
    }

    @Override
    public Booking findByVenueSlotIdAndStatus(Long id, String status) {
        return bookingMapper.findByVenueSlotIdAndStatus(id, status);
    }

    @Override
    public PageBean<Booking> listForClubAdmin(Integer pageNum, Integer pageSize, String status, Long clubId) {
        PageBean<Booking> pageBean = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Booking> list = bookingMapper.listForClubAdmin(status, clubId);
        Page p = (Page)list;
        pageBean.setTotal(p.getTotal());
        pageBean.setItems(p.getResult());
        return pageBean;
    }

    @Override
    public PageBean<Booking> list(Integer pageNum, Integer pageSize, String status, Long clubId) {
        PageBean<Booking> pageBean = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<Booking> list = bookingMapper.list( status, clubId);
        Page p = (Page)list;
        pageBean.setTotal(p.getTotal());
        pageBean.setItems(p.getResult());
        return pageBean;
    }
}
