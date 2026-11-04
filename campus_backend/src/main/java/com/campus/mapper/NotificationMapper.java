package com.campus.mapper;


import com.campus.entitys.Notification;
import com.campus.entitys.PageBean;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NotificationMapper {
    // 插入通知
    @Insert("INSERT INTO notification (user_id, message_id, type, title, content) VALUES (#{userId}, #{messageId}, #{type}, #{title}, #{content})")
    void add(Notification notification);
    //获取本人通知列表 （可根据readStatus查询未读或已读通知）
    @Select("SELECT * FROM notification WHERE user_id = #{userId} AND read_status = #{readStatus}")
    List<Notification> list(@Param("userId") Long userId, @Param("readStatus") Integer readStatus);
    // 根据ID标记通知为已读
    @Update("UPDATE notification SET read_status = 1 WHERE id = #{id}")
    void read(Long id);
}
