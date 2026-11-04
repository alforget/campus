package com.campus.vo;

import com.campus.entitys.Booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 场地预约返回对象。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingVO {
    private Long id;
    private Long slotId;
    private Long venueId;
    private String venueName;
    private String venueLocation;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long userId;
    private String username;
    private Long clubId;
    private String clubName;
    private String purpose;
    private String status;
    private String cancelReason;
    private Long cancelledBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BookingVO from(Booking booking, Long venueId, String venueName, String venueLocation,
                                 LocalDate slotDate, LocalTime startTime, LocalTime endTime,
                                 String username, String clubName) {
        if (booking == null) {
            return null;
        }
        BookingVO vo = new BookingVO();
        vo.setId(booking.getId());
        vo.setSlotId(booking.getSlotId());
        vo.setVenueId(venueId);
        vo.setVenueName(venueName);
        vo.setVenueLocation(venueLocation);
        vo.setSlotDate(slotDate);
        vo.setStartTime(startTime);
        vo.setEndTime(endTime);
        vo.setUserId(booking.getUserId());
        vo.setUsername(username);
        vo.setClubId(booking.getClubId());
        vo.setClubName(clubName);
        vo.setPurpose(booking.getPurpose());
        vo.setStatus(booking.getStatus());
        vo.setCancelReason(booking.getCancelReason());
        vo.setCancelledBy(booking.getCancelledBy());
        vo.setCreatedAt(booking.getCreatedAt());
        vo.setUpdatedAt(booking.getUpdatedAt());
        return vo;
    }
}
