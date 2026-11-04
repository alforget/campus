package com.campus.service.impl;

import com.campus.entitys.Venue;
import com.campus.mapper.VenueMapper;
import com.campus.service.VenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VenueServiceImpl implements VenueService {
    @Autowired
    private VenueMapper venueMapper;

    @Override
    public List<Venue> list() {
        return venueMapper.list();
    }

    @Override
    public void add(Venue venue) {
        venueMapper.add(venue);
    }

    @Override
    public void update(Venue venue) {
        venueMapper.update(venue);
    }

    @Override
    public void delete(Long id) {
        venueMapper.delete(id);
    }

    @Override
    public Venue detail(Long id) {
        return venueMapper.detail(id);
    }

    @Override
    public void updateStatus(Long id, int status) {
        venueMapper.updateStatus(id, status);
    }
}
