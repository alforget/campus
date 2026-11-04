package com.campus.vo;

import com.campus.entitys.Registration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 报名记录返回对象，列表场景下可带活动标题、用户名等冗余展示字段。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationVO {
    private Long id;
    private Long activityId;
    private String activityTitle;
    private LocalDateTime activityStartTime;
    private LocalDateTime activityEndTime;
    private String venueText;
    private Long userId;
    private String username;
    private String nickname;
    private String status;
    private String ticketNo;
    private String cancelReason;
    private Long cancelledBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RegistrationVO from(Registration registration, String activityTitle,
                                      LocalDateTime activityStartTime, LocalDateTime activityEndTime,
                                      String venueText, String username, String nickname) {
        if (registration == null) {
            return null;
        }
        RegistrationVO vo = new RegistrationVO();
        vo.setId(registration.getId());
        vo.setActivityId(registration.getActivityId());
        vo.setActivityTitle(activityTitle);
        vo.setActivityStartTime(activityStartTime);
        vo.setActivityEndTime(activityEndTime);
        vo.setVenueText(venueText);
        vo.setUserId(registration.getUserId());
        vo.setUsername(username);
        vo.setNickname(nickname);
        vo.setStatus(registration.getStatus());
        vo.setTicketNo(registration.getTicketNo());
        vo.setCancelReason(registration.getCancelReason());
        vo.setCancelledBy(registration.getCancelledBy());
        vo.setCreatedAt(registration.getCreatedAt());
        vo.setUpdatedAt(registration.getUpdatedAt());
        return vo;
    }
}
