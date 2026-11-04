package com.campus.service;

import com.campus.entitys.PageBean;
import com.campus.entitys.User;

public interface UserService {
    //添加用户
    void add(String username, String password,Long roleId);
    //根据用户名查询用户
    User fingByName(String username);
    //更新密码
    void updatePassword(String encodedPassword,String username);
    //更新用户信息
    void update(User user);
    //获取用户列表
    PageBean<User> list(Integer pageNum, Integer pageSize);
    //更新用户角色
    void updateRole(String username,Long roleId);
    //更新用户状态
    void updateState(String username, long l);
    //根据用户ID查询用户
    User findById(Long userId);
    //更新用户社团ID
    void updateClub(String username, Long clubId);
    //更新用户角色和社团ID
    void updateRoleAndClub(String username, long l, Long id);
}
