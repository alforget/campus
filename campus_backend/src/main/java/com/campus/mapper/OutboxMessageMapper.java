package com.campus.mapper;

import com.campus.entitys.OutboxMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OutboxMessageMapper {
    // 根据状态查询所有消息
    @Select("SELECT * FROM outbox_message WHERE status = '待投递' OR status = '失败'")
    List<OutboxMessage> findPendingOrFailed();
    // 更新消息状态
    @Update("UPDATE outbox_message SET status = #{newStatus} WHERE message_id = #{messageId} ")
    void updateStatus(@Param("messageId") String messageId, @Param("newStatus") String newStatus);
    // 添加消息
    @Insert("INSERT INTO outbox_message (message_id, message_type, payload) VALUES (#{messageId}, #{messageType}, #{payload})")
    int add(@Param("messageId") String messageId, @Param("messageType") String messageType, @Param("payload") String payload);
}
