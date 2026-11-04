package com.campus.entitys;
/**
 * 场地时段实体
 */

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
@Data
public class VenueSlot {
    private Long id;                  // 主键
    private Long venueId;             // 场地ID
    private LocalDate slotDate;       // 日期（原 date 重命名）
    private LocalTime startTime;      // 开始时间
    private LocalTime endTime;        // 结束时间
    private Integer capacity;         // 容量（现有，需求无，保留）
    private String status;            // OPEN/BOOKED/CLOSED（新增）
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间
}