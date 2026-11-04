package com.campus.mapper;

import com.campus.entitys.Club;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ClubMapper {
    // 创建社团
    @Insert("insert into club (name, create_time,update_time) " +
            "values (#{name}, now(),now())")
    void add(String clubName);
    // 更新社团信息
    @Update("update club set description=#{description},logo_url=#{logoUrl},update_time=now() where id=#{id} and leader_id=#{currentUserId}")
    void update(Club club,@Param("currentUserId") Long currentUserId);
    // 禁用社团
    @Update("update club set status=0,update_time=now() where name=#{clubName}")
    void disable(String clubName);
    // 启用社团
    @Update("update club set status=1,update_time=now() where name=#{clubName}")
    void enable(String clubName);
    // 查询所有社团
    @Select("select * from club ")
    List<Club> list();
    // 根据名称查询社团
    @Select("select * from club where name=#{clubName}")
    Club findByName(String clubName);
    // 为社团分配管理员 (管理员权限)
    @Update("update club set leader_id=#{userId},update_time=now() where name=#{clubName}")
    void assignAdmin(@Param("clubName") String clubName, @Param("userId") Long userId);
    // 删除社团管理员 (管理员权限)
    @Update("update club set leader_id=null,update_time=now() where name=#{clubName}")
    void deleteAdmin(String clubName);
    // 查询社团详情
    @Select("select * from club where name=#{clubName}")
    Club detail(String clubName);
    // 更新社团预约数量
    @Update("update club set booking_numbers=booking_numbers+1 where id=#{clubId}")
    int addBookingNumbers(Long clubId);
    // 减少社团预约数量
    @Update("update club set booking_numbers=booking_numbers-1 where id=#{clubId}")
    void reduceBookingNumbers(Long clubId);
    // 查询社团预约数量
    @Select("select booking_numbers from club where id=#{clubId}")
    int getBookingNumbers(Long clubId);
}
