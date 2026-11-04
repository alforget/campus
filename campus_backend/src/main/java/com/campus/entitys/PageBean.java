package com.campus.entitys;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/*
分页返回结果对象
 */
@Data
@NoArgsConstructor//生成无参构造器
@AllArgsConstructor//生成全参构造器
public class PageBean <T>{
    private Long total;//总条数
    private List<T> items;//当前页数据集合
}
