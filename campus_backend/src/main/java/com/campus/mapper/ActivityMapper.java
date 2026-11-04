package com.campus.mapper;

import com.campus.entitys.Activity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityMapper {
    // 添加活动
    @Insert("INSERT INTO activity (title, club_id,summary,description, cover_url, venue_id, capacity, remaining, waitlist_enabled,waitlist_limit,waitlist_remaining,signup_start, " +
            "signup_end,start_time,end_time, created_at, updated_at, created_by) VALUES (#{title}, #{clubId},  #{summary}, #{description}, #{coverUrl}, #{venueId}," +
            " #{capacity}, #{capacity}, #{waitlistEnabled}, #{waitlistLimit}, #{waitlistLimit}, #{signupStart}, #{signupEnd}, #{startTime}, #{endTime},  now(), now(), #{createdBy})")
    void add(Activity activity);

    //获得活动列表
    List<Activity> list(@Param("status") String status, @Param("clubId") Long clubId,@Param("isArchive") Boolean isArchive);

    //根据活动ID查询活动
    @Select("SELECT * FROM activity WHERE id = #{id}")
    Activity findById(Long id);

    //驳回活动
    @Update("UPDATE activity SET  " +
            " version = version+1, reject_reason = #{rejectReason}, updated_at = now(), status = #{status} " +
            "WHERE id = #{id} and version=#{version}")
    int reject(Activity activity);

    //更新活动
    @Update("UPDATE activity SET title = #{title},summary = #{summary},description = #{description}, cover_url = #{coverUrl}, venue_id = #{venueId}, capacity = #{capacity}, " +
            " version = version+1, reject_reason = #{rejectReason}, waitlist_enabled = #{waitlistEnabled}, waitlist_limit = #{waitlistLimit}, waitlist_remaining = #{waitlistRemaining}, " +
            " signup_start = #{signupStart}, signup_end = #{signupEnd},start_time = #{startTime}, end_time = #{endTime}, status = '草稿' " +
            "WHERE id = #{id} and version=#{version} and created_by=#{userId}")
    int updatedetail(Activity activity, @Param("userId") Long userId);

    //获得活动列表（学生端）
    @Select("SELECT * FROM activity WHERE status != '草稿' AND status != '已驳回' AND status != '已取消' AND status != '待审核' ")
    List<Activity> listForStudent();

    //更新活动状态
    @Update("UPDATE activity SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("status") String status, @Param("id") Long id);

    //报名活动
    @Update("UPDATE activity SET remaining = remaining - 1 WHERE id = #{activityId} and remaining>0")
    int register(Long activityId);

    //更新活动容量
    @Update("UPDATE activity SET remaining = remaining + 1 WHERE id = #{activityId} and remaining<capacity")
       int adjustRemaining(Long activityId);

    //查询应改为报名中的活动
    @Select("SELECT * FROM activity WHERE status = '已发布' AND signup_start <= now() AND signup_end >= now()")
    List<Activity> selectSignUp();

    @Flush
    List<Object> flush();

    //查询应改为已开始的活动
    @Select("SELECT * FROM activity WHERE status = '报名已结束' AND start_time <= now() AND end_time >= now()")
    List<Activity> selectStart();

    //查询应改为已结束的活动
    @Select("SELECT * FROM activity WHERE status = '已开始' AND end_time <= now()")
    List<Activity> selectEnd();

    //查询应改为报名已结束的活动
    @Select("SELECT * FROM activity WHERE status = '报名中' AND signup_end <= now() ")
    List<Activity> selectSignupEnd();

    //查询应改为已归档的活动
    @Select("SELECT * FROM activity WHERE is_archive = false and end_time <= now()-interval 1 day and status = '已结束'")
    List<Activity> selectArchive();

    //更新活动是否归档
    @Update("UPDATE activity SET is_archive = #{isArchive} WHERE id = #{id}")
    int updateArchive(@Param("isArchive") boolean isArchive, @Param("id") Long id);

    //提交活动
    @Update("UPDATE activity SET status = #{status},  updated_at = now() WHERE id = #{id}  and created_by=#{userId}")
    int submit(@Param("id") Long id, @Param("userId") Long userId, @Param("status") String status);
    //获得活动列表 (社团管理员端)
    List<Activity> listForClub(@Param("status") String status, @Param("isArchive") Boolean isArchive, @Param("clubId") Long clubId);
    //删除活动 仅管理员可以删除
    @Delete("DELETE FROM activity WHERE id = #{id} AND status != '草稿' AND status != '已驳回' AND status != '已取消' AND status != '待审核'")
    void delete(Long id);
    //添加候补
    @Update("UPDATE activity SET waitlist_remaining = waitlist_remaining - 1 WHERE id = #{activityId} and waitlist_remaining>0")
    int addWaitList(Long activityId);
    //增加候补名额
    @Update("UPDATE activity SET waitlist_remaining = waitlist_remaining + 1 WHERE id = #{activityId} and waitlist_remaining<waitlist_limit")
    int adjustWaitList(Long activityId);
    //查询需要补位的活动ID
    @Select("SELECT id FROM activity WHERE remaining > 0 AND id IN (SELECT activity_id FROM registration WHERE status = '候补')")
    List<Long> findActivitiesNeedCompensate();
    //根据活动ID和社团ID查询活动详情（社团管理员端）
    @Select("SELECT * FROM activity WHERE id = #{id} AND club_id = #{clubId}")
    Activity findByIdForClubAdmin(@Param("id") Long id, @Param("clubId") Long clubId);
    //根据活动ID查询活动详情（学生端）
    @Select("SELECT * FROM activity WHERE id = #{id} AND status != '草稿' AND status != '已驳回' AND status != '已取消' AND status != '待审核'")
    Activity findByIdForStudent(Long id);
    //更新活动余额
    @Update("UPDATE activity SET remaining = #{remaining} WHERE id = #{id}")
    int updateRemaining(@Param("id") Long id, @Param("remaining") int remaining);
    // 直接联表查询出数据不一致的活动
    @Select("SELECT a.id, a.title, a.capacity, a.remaining, " +
            "(SELECT COUNT(1) FROM registration r WHERE r.activity_id = a.id AND r.status = '成功') AS success_count " +
            "FROM activity a " +
            "HAVING a.remaining < 0 OR a.remaining != (a.capacity - success_count)")
    List<Map<String, Object>> checkActivityBalance();
}
