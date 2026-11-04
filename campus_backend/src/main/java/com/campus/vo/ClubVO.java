package com.campus.vo;

import com.campus.entitys.Club;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 社团返回对象。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClubVO {
    private Long id;
    private String name;
    private Long leaderId;
    private String leaderName;
    private String description;
    private String logoUrl;
    private Integer status;
    private Integer bookingNumbers;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ClubVO from(Club club, String leaderName) {
        if (club == null) {
            return null;
        }
        ClubVO vo = new ClubVO();
        vo.setId(club.getId());
        vo.setName(club.getName());
        vo.setLeaderId(club.getLeaderId());
        vo.setLeaderName(leaderName);
        vo.setDescription(club.getDescription());
        vo.setLogoUrl(club.getLogoUrl());
        vo.setStatus(club.getStatus());
        vo.setBookingNumbers(club.getBookingNumbers());
        vo.setCreateTime(club.getCreateTime());
        vo.setUpdateTime(club.getUpdateTime());
        return vo;
    }
}
