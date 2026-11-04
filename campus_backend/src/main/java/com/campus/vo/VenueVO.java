package com.campus.vo;

import com.campus.entitys.Venue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 场馆返回对象。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VenueVO {
    private Long id;
    private String name;
    private String location;
    private Integer capacity;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static VenueVO from(Venue venue) {
        if (venue == null) {
            return null;
        }
        VenueVO vo = new VenueVO();
        vo.setId(venue.getId());
        vo.setName(venue.getName());
        vo.setLocation(venue.getLocation());
        vo.setCapacity(venue.getCapacity());
        vo.setDescription(venue.getDescription());
        vo.setStatus(venue.getStatus());
        vo.setCreatedAt(venue.getCreatedAt());
        vo.setUpdatedAt(venue.getUpdatedAt());
        return vo;
    }
}
