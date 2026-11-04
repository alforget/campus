package com.campus.mapper;

import com.campus.entitys.Registration;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RegistrationMapper {
    //添加报名
    @Insert("insert into registration (activity_id,user_id,status,ticket_no,idempotency_key,created_at,updated_at) " +
            "values (#{activityId}, #{userId}, #{status}, #{ticketNo}, #{idempotencyKey}, now(), now()) ON DUPLICATE KEY UPDATE id = id")
    int add(Registration registration);

    //更新报名状态
    @Update("update registration set status = #{status} where id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    //根据报名ID查询报名详情
    @Select("select * from registration where id = #{id}")
    Registration findById(Long id);

    //删除报名
    @Delete("delete from registration where id = #{id}")
    int delete(Long id);

    //根据活动ID获取报名列表
    List<Registration> list(@Param("activityId") Long activityId, @Param("status") String status);

    //根据活动ID删除所有报名
    @Delete("delete from registration where activity_id = #{activityId}")
    void deleteActivity(Long activityId);

    //根据用户ID和活动ID查询报名票号
    @Select("select * from registration where user_id = #{userId} and activity_id = #{activityId}")
    Registration findByUserIdAndActivityId(@Param("userId") Long userId, @Param("activityId") Long activityId);

    @Flush
    List<Object> flush();

    //查询已归档活动的报名
    @Select("select * from registration where activity_id in (select id from activity where is_archive = true)")
    List<Registration> selectArchive();

    //更新报名状态为已归档
    @Update("update registration set is_archive = #{b} where id = #{id}")
    void updateArchive(@Param("id") Long id, @Param("b") boolean b);

    //查询已结束但未归档的活动且未签到的报名
    @Select("select * from registration where activity_id in (select id from activity where status = '已结束') and is_archive = false" +
            " and (select status from checkin where registration_id = registration.id) != 1")
    List<Registration> selectEnd();

    //根据活动ID和状态查询报名列表
    @Select("select * from registration where activity_id = #{activityId} and status = #{status}")
    List<Registration> findByActivityIdAndStatus(@Param("activityId") Long activityId, @Param("status") String status);

    //取消报名（新增）
    @Update("update registration set status = '取消', cancel_reason = #{cancelReason}, cancelled_by = #{cancelledBy}, updated_at = now() " +
            "where id = #{registrationId} ")
    void cancel(@Param("registrationId") Long registrationId, @Param("cancelledBy") Long cancelledBy, @Param("cancelReason") String cancelReason);

    //根据用户ID查询报名列表（学生）
    List<Registration> listForStudent(@Param("activityId") Long activityId, @Param("status") String status, @Param("userId") Long userId);
    //全量查询报名列表
    List<Registration> listAll(@Param("activityId") Long activityId, @Param("status") String status);
   //根据幂等键查询报名
    @Select("select * from registration where idempotency_key = #{idempotencyKey}")
    Registration findByIdempotencyKey(String idempotencyKey);
    //根据用户ID和票号查询报名详情
    @Select("select * from registration where user_id = #{userId} and ticket_no = #{ticketNo}")
    Registration findByUserIdAndTicketNo(@Param("userId") Long userId, @Param("ticketNo") String ticketNo);
    //获取报名总人数
    @Select("select count(*) from registration where activity_id = #{activityId}")
    int count(Long activityId);
    //根据活动ID查询所有报名用户ID
    @Select("select user_id from registration where activity_id = #{activityId} ")
    List<Long> findAllByActivityId(Long activityId);
    //根据活动ID查询报名成功的人数
    @Select("select count(*) from registration where activity_id = #{activityId} and (status = '成功' or status = '已签到' or status = '未出席')")
    Integer countByActivityId(Long activityId);
    //批量取消报名
    @Update("<script>" +
            "UPDATE registration SET status = '已取消' " +
            "WHERE activity_id = #{activityId} AND user_id IN " +
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>" +
            "#{uid}" +
            "</foreach>" +
            "</script>")
    void batchCancelRegistrations(@Param("activityId") Long activityId, @Param("userIds") List<Long> userIds);
}