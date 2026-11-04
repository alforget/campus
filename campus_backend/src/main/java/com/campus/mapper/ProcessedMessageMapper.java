package com.campus.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProcessedMessageMapper {
    // 插入已处理记录
    @Insert("INSERT INTO processed_message (message_id,consumer) VALUES (#{messageId},#{consumer})")
    void add(@Param("messageId") String messageId, @Param("consumer") String consumer);
     // 根据消息ID检查是否存在已处理记录
    @Select("SELECT COUNT(*) FROM processed_message WHERE message_id = #{messageId} AND consumer = #{consumer}")
    int existsById(@Param("messageId") String messageId, @Param("consumer") String consumer);
}
