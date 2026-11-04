package com.campus.service.impl;

import com.campus.entitys.PageBean;
import com.campus.entitys.User;
import com.campus.mapper.UserMapper;
import com.campus.service.UserService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class UserServiceImpl implements UserService {
     @Autowired
     private UserMapper userMapper;
     @Autowired
     private PasswordEncoder passwordEncoder;

     @Override
     public void add(String username, String password,Long roleId) {
          String encodedPassword = passwordEncoder.encode(password);
          userMapper.add(username, encodedPassword,roleId);
     }

     @Override
     public User fingByName(String username) {
          return userMapper.findByName(username);
     }

     @Override
     public void updatePassword(String encodedPassword, String username) {
          userMapper.updatePassword(encodedPassword, username);
     }

     @Override
     public void update(User user) {
          userMapper.update(user);
     }

     @Override
     public PageBean<User> list(Integer pageNum, Integer pageSize) {
          PageBean<User> pb = new PageBean<>();
          //分页查询
          PageHelper.startPage(pageNum, pageSize);
          List<User> users = userMapper.list();
          Page<User> usersPage = (Page) users;
          pb.setTotal(usersPage.getTotal());
          pb.setItems(usersPage.getResult());
          return pb;
     }

     @Override
     public void updateRole(String username, Long roleId) {
          userMapper.updateRole(username, roleId);
     }

     @Override
     public void updateState(String username, long l) {
          userMapper.updateState(username, l);
     }

     @Override
     public User findById(Long userId) {
          return userMapper.findById(userId);
     }

     @Override
     public void updateClub(String username, Long clubId) {
          userMapper.updateClub(username, clubId);
     }

     @Override
     public void updateRoleAndClub(String username, long l, Long id) {
          userMapper.updateRoleAndClub(username, l, id);
     }

}
