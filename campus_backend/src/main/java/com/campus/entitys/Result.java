package com.campus.entitys;

import com.campus.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> implements Serializable {
    private Integer code;//业务状态码  0-成功  1-失败
    private String msg;//提示信息
    private T data;//响应数据

    //快速返回操作成功响应结果(带响应数据)
    public static <E> Result<E> success(E data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), "操作成功", data);
    }

    //快速返回操作成功响应结果
    public static Result success() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), "操作成功", null);
    }

    //快速返回操作失败响应结果(带状态码)
    public static Result error(Integer code, String msg) {
        return new Result(code, msg, null);
    }

    //快速返回操作失败响应结果(带状态码和错误码)
    public static <T> Result<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

    //快速返回操作失败响应结果(带状态码和错误信息)
    public static <T> Result<T> error(String msg) {
        return error(40000, msg); // 默认业务错误
    }
}
