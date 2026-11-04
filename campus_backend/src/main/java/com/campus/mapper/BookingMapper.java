package com.campus.mapper;

import com.campus.entitys.Booking;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BookingMapper {
    //添加预约
    @Insert("INSERT INTO booking (user_id,slot_id,club_id,purpose, created_at, updated_at) " +
            "VALUES (#{userId}, #{slotId}, #{clubId}, #{purpose}, now(), now())")
    void add(Booking booking);

    //根据ID查询预约
    @Select("SELECT * FROM booking WHERE id = #{id}")
    Booking findById(Long id);

    //更新预约
    @Update("UPDATE booking SET slot_id = #{slotId}, club_id = #{clubId}, purpose = #{purpose}, " +
            "updated_at = now(), version =version +1 WHERE id = #{id} AND version = #{version} AND user_id = #{userId}")
    int update(Booking booking);

    //驳回预约
    @Update("UPDATE booking SET reject_reason = #{rejectReason}, status = '已驳回', updated_at = now(), version =version +1 WHERE id = #{id} AND version = #{version} ")
    int updateRejectReason(@Param("id") Long id, @Param("rejectReason") String rejectReason, @Param("version") Integer version);

    //通过预约
    @Update("UPDATE booking SET status = '已通过', updated_at = now(), version =version +1 WHERE id = #{id} AND version = #{version} ")
    int updateApprove(@Param("id") Long id, @Param("version") Integer version);
    //提交预约
    @Update("UPDATE booking SET status = '已通过', updated_at = now(), version =version +1 WHERE id = #{id} AND version = #{version} and user_id = #{userId} ")
    int submit(@Param("id") Long id, @Param("version") Integer version,@Param("userId") Long userId);
    //取消预约
    @Update("UPDATE booking SET cancel_reason = #{cancelReason}, status = '已取消', cancelled_by = #{cancelledBy}, updated_at = now(), version =version +1 WHERE id = #{id} AND version = #{version} AND user_id = #{cancelledBy} ")
    int updateCancelReason(@Param("id") Long id, @Param("cancelReason") String cancelReason, @Param("version") Integer version, @Param("cancelledBy") Long cancelledBy);
    //取消预约 (场地管理员)
    @Update("UPDATE booking SET cancel_reason = #{cancelReason}, status = '已取消', cancelled_by = #{cancelledBy}, updated_at = now(), version =version +1 WHERE id = #{id} AND version = #{version} ")
    int updateCancelReason2(@Param("id") Long id, @Param("cancelReason") String cancelReason, @Param("version") Integer version, @Param("cancelledBy") Long cancelledBy);

    //根据场地时段ID和状态查询预约
    @Select("SELECT * FROM booking WHERE slot_id = #{id} AND status = #{status}")
    Booking findByVenueSlotIdAndStatus(@Param("id") Long id, @Param("status") String status);
    //根据社团ID或状态查询预约 （仅社团管理员可操作）
    List<Booking> listForClubAdmin( @Param("status") String status, @Param("clubId") Long clubId);
    //根据社团ID或状态查询预约 （仅场地管理员可操作）
    List<Booking> list( @Param("status") String status, @Param("clubId") Long clubId);

    //查询所有预约  测试使用
    @Select("SELECT * FROM booking")
    List<Booking> listAll();

}
