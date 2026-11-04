package com.campus.entitys;

import lombok.Data;

@Data
public class RegistrationMessage {
    private Long userId;             // 用户ID
    private String ticketNo;         // 票号
    private String userName;         // 用户名
    private String email;            // 邮箱
    private String phone;            // 手机号
}
