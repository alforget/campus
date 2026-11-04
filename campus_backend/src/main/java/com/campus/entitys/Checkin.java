package com.campus.entitys;
/**
 * 签到实体
 */

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class Checkin {
    private Long id;// 主键
    @JsonIgnore
    private Long registrationId;      // 报名ID，唯一（新增）
    @JsonIgnore
    private Long activityId;          // 活动ID（冗余保留）
    private Long userId;              // 用户ID（冗余保留）
    private Integer status;           // 0未签到 1已签到 （现有，需求无，保留）
    private LocalDateTime checkedAt;// 签到时间（原 checkin_at 重命名）
    @JsonIgnore
    private Long operatorId;          // 操作人ID（冗余保留）
}