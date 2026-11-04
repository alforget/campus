package com.campus.vo;

import com.campus.entitys.Activity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动列表/详情返回对象。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityVO {
    private Long id;
    private Long clubId;
    private String clubName;
    private Long venueId;
    private String venueName;
    private String venueText;
    private String title;
    private String summary;
    private String description;
    private String coverUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime signupStart;
    private LocalDateTime signupEnd;
    private Integer capacity;
    private Integer remaining;
    private Integer waitlistEnabled;
    private Integer waitlistLimit;
    private Integer waitlistRemaining;
    private String status;
    private String rejectReason;
    private Long createdBy;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isArchive;

    public static ActivityVO from(Activity activity, String clubName, String venueName) {
        if (activity == null) {
            return null;
        }
        ActivityVO vo = new ActivityVO();
        vo.setId(activity.getId());
        vo.setClubId(activity.getClubId());
        vo.setClubName(clubName);
        vo.setVenueId(activity.getVenueId());
        vo.setVenueName(venueName);
        vo.setVenueText(activity.getVenueText());
        vo.setTitle(activity.getTitle());
        vo.setSummary(activity.getSummary());
        vo.setDescription(activity.getDescription());
        vo.setCoverUrl(activity.getCoverUrl());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setSignupStart(activity.getSignupStart());
        vo.setSignupEnd(activity.getSignupEnd());
        vo.setCapacity(activity.getCapacity());
        vo.setRemaining(activity.getRemaining());
        vo.setWaitlistEnabled(activity.getWaitlistEnabled());
        vo.setWaitlistLimit(activity.getWaitlistLimit());
        vo.setWaitlistRemaining(activity.getWaitlistRemaining());
        vo.setStatus(activity.getStatus());
        vo.setRejectReason(activity.getRejectReason());
        vo.setCreatedBy(activity.getCreatedBy());
        vo.setVersion(activity.getVersion());
        vo.setCreatedAt(activity.getCreatedAt());
        vo.setUpdatedAt(activity.getUpdatedAt());
        vo.setIsArchive(activity.getIsArchive());
        return vo;
    }
}
