package com.campus.scheduled_task;

import com.campus.entitys.Venue;
import com.campus.entitys.VenueSlot;
import com.campus.service.VenueService;
import com.campus.service.VenueSlotService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class AutoAdjustVenueSlot {
    @Autowired
    private VenueSlotService venueSlotService;
    @Autowired
    private VenueService venueService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void adjustVenueSlot() {
        //分配今天往后第14天的时段
        List<Venue> venues = venueService.list();
        for (int i = 0; i < venues.size(); i++) {
            Venue venue = venues.get(i);
            //如果场馆状态为禁用，跳过
            if (venue.getStatus() == 0) {
                continue;
            }
            for (int k = 8; k < 22; k += 2) {
                VenueSlot venueSlot = new VenueSlot();
                venueSlot.setVenueId(venue.getId());
                venueSlot.setSlotDate(LocalDate.now().plusDays(13));
                venueSlot.setStartTime(LocalTime.of(k, 0));
                venueSlot.setEndTime(LocalTime.of(k + 2, 0));
                venueSlot.setCapacity(1);
                venueSlotService.add(venueSlot);
            }
        }


    }


}
