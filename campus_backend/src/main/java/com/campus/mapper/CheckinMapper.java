package com.campus.mapper;

import com.campus.entitys.Checkin;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CheckinMapper {
    //添加签到记录
    @Insert("insert into checkin (user_id,activity_id,status,registration_id) values (#{userId}, #{activityId}, 1,#{registrationId})")
    int add(@Param("userId") Long userId, @Param("activityId") Long activityId, @Param("registrationId") Long registrationId);
    //获取所有签到记录
    @Select("select * from checkin")
    List<Checkin> list();
    //更新签到状态
    @Update("update checkin set status = 1, checkin_at = now() where user_id = #{userId} and activity_id = #{activityId}")
    void updateStatus(@Param("userId") Long userId, @Param("activityId") Long activityId);
    //删除签到记录
    @Delete("delete from checkin where user_id = #{userId} and activity_id = #{activityId}")
    int delete(@Param("userId") Long userId, @Param("activityId") Long activityId);
    //根据报名ID查询签到记录
    @Select("select * from checkin where registration_id = #{registrationId}")
    Checkin findByRegistrationId(Long registrationId);
    //获取签到统计
    @Select("select count(*) from checkin where activity_id = #{activityId} and status = 1")
    int count(@Param("activityId") Long activityId);

}
