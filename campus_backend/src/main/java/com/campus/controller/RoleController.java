package com.campus.controller;

import com.campus.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/role")
public class RoleController {
    @Autowired
    private RoleService roleService;

    //根据角色名获取角色ID
    @GetMapping("/getRoleId")
    public Long getRoleId(String roleName) {
        return roleService.getRoleId(roleName);
    }
}
