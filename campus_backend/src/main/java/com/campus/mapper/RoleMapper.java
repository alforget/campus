package com.campus.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper {
    //根据角色名获取角色ID
    @Select("select id from role where name = #{roleName}")
    Long getRoleId(String roleName);
}
