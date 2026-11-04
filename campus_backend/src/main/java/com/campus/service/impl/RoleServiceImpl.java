package com.campus.service.impl;

import com.campus.mapper.RoleMapper;
import com.campus.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl implements RoleService {
     @Autowired
    private RoleMapper roleMapper;

     @Override
    public Long getRoleId(String roleName) {
        return roleMapper.getRoleId(roleName);
    }
}
