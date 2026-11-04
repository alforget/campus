package com.campus.entitys;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Club {
    private Long id;//主键ID
    private String name;//社团名称
    private Long leaderId;//社团负责人ID
    private String description;//社团描述
    private String logoUrl;//logo地址
    private Integer status;//状态：1启用，0禁用
    private Integer bookingNumbers;//待开始的预约数量
    private LocalDateTime createTime;//创建时间
    private LocalDateTime updateTime;//更新时间
}
