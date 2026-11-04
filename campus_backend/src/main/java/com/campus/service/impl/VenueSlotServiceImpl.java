package com.campus.service.impl;

import com.campus.entitys.PageBean;
import com.campus.entitys.VenueSlot;
import com.campus.mapper.VenueSlotMapper;
import com.campus.service.VenueSlotService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class VenueSlotServiceImpl implements VenueSlotService {
    @Autowired
    private VenueSlotMapper venueSlotMapper;

    @Override
    public void add(VenueSlot venueSlot) {
        venueSlotMapper.add(venueSlot);
    }

    @Override
    public void deleteBySlotDate(LocalDate localDate) {
        venueSlotMapper.deleteBySlotDate(localDate);
    }

    @Override
    public VenueSlot findById(Long slotId) {
        return venueSlotMapper.findById(slotId);
    }

    @Override
    public int updateStatus(Long slotId, String status) {
        return venueSlotMapper.updateStatus(slotId, status);
    }

    @Override
    public List<VenueSlot> findByVenueIdAndTime(Long venueId, LocalDate slotDate, LocalTime startTime, LocalTime endTime) {
        return venueSlotMapper.findByVenueIdAndTime(venueId, slotDate, startTime, endTime);
    }


    @Override
    public PageBean<VenueSlot> list(Integer pageNum, Integer pageSize, Long id, LocalDate slotDate) {
        PageBean<VenueSlot> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<VenueSlot> list = venueSlotMapper.findByVenueIdAndSlotDate(id, slotDate);
        Page page = (Page) list;
        pb.setTotal(page.getTotal());
        pb.setItems(page.getResult());
        return pb;
    }

    @Override
    public int updateStatusToBOOKED(Long slotId) {
        return venueSlotMapper.updateStatusToBOOKED(slotId);
    }
}
