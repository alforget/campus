package com.campus.mapper;

import com.campus.entitys.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    //添加用户
    @Insert("insert into user (username, password_hash,role_id,created_at,updated_at) " +
            "values (#{username}, #{encodedPassword},#{roleId},now(),now())")
    void add(@Param("username") String username, @Param("encodedPassword") String encodedPassword,@Param("roleId") Long roleId);

    //根据用户名查询用户
    @Select("select * from user where username = #{username}")
    User findByName(String username);

    //更新密码
    @Update("update user set password_hash = #{encodedPassword},updated_at = now() where username = #{username}")
    void updatePassword(@Param("encodedPassword") String encodedPassword, @Param("username") String username);

    //更新用户信息
    @Update("update user set nickname = #{nickname},email = #{email},avatar_url = #{avatarUrl},phone = #{phone},last_login_at = now(),updated_at = now() " +
            "where id = #{id} ")
    void update(User user);

    //获取用户列表
    @Select("select * from user")
    List<User> list();

    //更新用户角色
    @Update("update user set role_id = #{roleId},updated_at = now() where username = #{username}")
    void updateRole(@Param("username") String username, @Param("roleId") Long roleId);

    //更新用户状态
    @Update("update user set status = #{status},updated_at = now() where username = #{username}")
    void updateState(@Param("username") String username, @Param("status") Long status);

    //根据用户ID查询用户
    @Select("select * from user where id = #{userId}")
    User findById(Long userId);

    //更新用户社团ID
    @Update("update user set club_id = #{clubId},updated_at = now() where username = #{username}")
    void updateClub(@Param("username") String username, @Param("clubId") Long clubId);

    //更新用户角色和社团ID
    @Update("update user set role_id = #{roleId},club_id = #{clubId},updated_at = now() where username = #{username}")
    void updateRoleAndClub(@Param("username") String username, @Param("roleId") Long roleId, @Param("clubId") Long clubId);
}
