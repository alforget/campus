package com.campus.mapper;

import com.campus.entitys.Venue;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VenueMapper {
    //获取全部数据
    @Select("select * from venue")
    List<Venue> list();

    //添加场馆 （仅场地管理员或管理员可操作）
    @Insert("insert into venue (name, location, capacity, description) values (#{name}, #{location}, #{capacity}, #{description})")
    void add(Venue venue);
    //编辑场馆 （仅场地管理员或管理员可操作）
    @Update("update venue set location = #{location}, capacity = #{capacity}, description = #{description} where id = #{id}")
    void update(Venue venue);
    //删除场馆 （仅场地管理员或管理员可操作）
    @Delete("delete from venue where id = #{id}")
    void delete(Long id);
    //根据ID查询场馆详情
    @Select("select * from venue where id = #{id}")
    Venue detail(Long id);
    //更新场馆状态
    @Update("update venue set status = #{status} where id = #{id}")
    void updateStatus(Long id, int status);
}
