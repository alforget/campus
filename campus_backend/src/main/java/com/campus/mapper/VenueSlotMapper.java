package com.campus.mapper;

import com.campus.entitys.VenueSlot;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface VenueSlotMapper {
    //添加场地时段（初始分配）
    @Insert("INSERT IGNORE INTO venue_slot (venue_id, slot_date, start_time, end_time, " +
            "capacity ,created_at, updated_at) VALUES (#{venueId}, #{slotDate}," +
            " #{startTime}, #{endTime}, #{capacity},now(),now())")
    void add(VenueSlot venueSlot);

    //删除指定日期的时段
    @Delete("DELETE FROM venue_slot WHERE slot_date = #{localDate}")
    void deleteBySlotDate(LocalDate localDate);
    //根据ID查询场地时段
    @Select("SELECT * FROM venue_slot WHERE id = #{slotId}")
    VenueSlot findById(Long slotId);
    // 更新场地时段状态
    @Update("UPDATE venue_slot SET status = #{status} WHERE id = #{slotId}")
    int updateStatus(@Param("slotId") Long slotId, @Param("status") String status);
    //根据场地ID查询场地时段
    @Select("SELECT * FROM venue_slot WHERE venue_id = #{venueId}")
    VenueSlot findByVenueId(Long venueId);
    //根据场地ID和日期查询指定时间范围的时段
    @Select("SELECT * FROM venue_slot WHERE venue_id = #{venueId} AND slot_date = #{slotDate} AND start_time <= #{startTime} AND end_time >= #{endTime}")
    List<VenueSlot> findByVenueIdAndTime(@Param("venueId") Long venueId, @Param("slotDate") LocalDate slotDate, @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);
    //根据场地ID和日期查询时段
    @Select("SELECT * FROM venue_slot WHERE venue_id = #{venueId} AND slot_date = #{slotDate}")
    List<VenueSlot> findByVenueIdAndSlotDate(@Param("venueId") Long venueId, @Param("slotDate") LocalDate slotDate);
    //根据场地ID和日期范围查询已存在的时段键
    @Select("SELECT CONCAT(slot_date, '_', HOUR(start_time)) FROM venue_slot WHERE venue_id = #{venueId} AND slot_date BETWEEN #{startDate} AND #{endDate}")
    List<String> findExistingKeys(@Param("venueId") Long venueId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    // 更新场地时段状态为已预约
    @Update("UPDATE venue_slot SET status = 'BOOKED' WHERE id = #{slotId} AND status = 'OPEN'")
    int updateStatusToBOOKED(Long slotId);
}
