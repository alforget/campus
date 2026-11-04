package com.campus.vo;

import com.campus.entitys.VenueSlot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 场地时段返回对象。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VenueSlotVO {
    private Long id;
    private Long venueId;
    private String venueName;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static VenueSlotVO from(VenueSlot slot, String venueName) {
        if (slot == null) {
            return null;
        }
        VenueSlotVO vo = new VenueSlotVO();
        vo.setId(slot.getId());
        vo.setVenueId(slot.getVenueId());
        vo.setVenueName(venueName);
        vo.setSlotDate(slot.getSlotDate());
        vo.setStartTime(slot.getStartTime());
        vo.setEndTime(slot.getEndTime());
        vo.setCapacity(slot.getCapacity());
        vo.setStatus(slot.getStatus());
        vo.setCreatedAt(slot.getCreatedAt());
        vo.setUpdatedAt(slot.getUpdatedAt());
        return vo;
    }
}
