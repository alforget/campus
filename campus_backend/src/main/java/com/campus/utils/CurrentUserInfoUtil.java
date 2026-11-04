package com.campus.utils;

import java.util.Map;

public class CurrentUserInfoUtil {

    //获取当前用户角色ID
    public static Long getRoleId(){
        Map<String, Object> claims = ThreadLocalUtil.get();
        //从claims中获取roleId
        Long roleId = Long.valueOf(claims.get("roleId").toString());
        return roleId;
    }

    //获取当前用户ID
    public static Long getUserId(){
        Map<String, Object> claims = ThreadLocalUtil.get();
        //从claims中获取userId
        Long userId = Long.valueOf(claims.get("userId").toString());
        return userId;
    }

    //获取当前用户角色名称
    public static String getUserName(){
        Map<String, Object> claims = ThreadLocalUtil.get();
        //从claims中获取userName
        String userName = (String) claims.get("username");
        return userName;
    }
}
