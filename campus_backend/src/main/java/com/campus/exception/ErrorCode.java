package com.campus.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 成功
    SUCCESS(200, "操作成功", 200),

    // 通用错误
    PARAM_ERROR(400, "参数校验失败", 400),
    UNAUTHORIZED(401, "未登录或登录已过期", 401),
    FORBIDDEN(403, "没有权限执行此操作", 403),
    NOT_FOUND(404, "请求的资源不存在", 404),

    // 业务错误 (可以把业务码设为 4xxxx，方便前端区分)
    BUSINESS_ERROR(40000, "业务处理异常", 400),
    BOOKING_FULL(40001, "预约名额已满", 400),

    // 系统错误
    SYSTEM_ERROR(50000, "系统内部错误，请稍后重试", 500),
    SQL_ERROR(50001, "数据库操作失败", 500);

    private final int code;       // 业务错误码
    private final String message; // 提示信息
    private final int httpStatus; // 对应的 HTTP 状态码

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}