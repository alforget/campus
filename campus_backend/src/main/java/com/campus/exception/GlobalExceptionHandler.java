package com.campus.exception;

import com.campus.entitys.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 1. 处理业务异常（我们主动抛出的）
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(Result.error(errorCode.getCode(), e.getMessage()));
    }

    /**
     * 2. 处理参数校验异常 (@Valid 或 @Validated 触发)
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleValidationException(Exception e) {
        String message = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException ex) {
            FieldError fieldError = ex.getBindingResult().getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        } else if (e instanceof BindException ex) {
            FieldError fieldError = ex.getBindingResult().getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        }
        log.warn("参数校验异常: {}", message);
        return ResponseEntity.status(ErrorCode.PARAM_ERROR.getHttpStatus())
                .body(Result.error(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 3. 处理数据库异常 (重点：不能把 SQL 暴露给前端)
     */
    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public ResponseEntity<Result<Void>> handleSqlException(Exception e) {
        // ⚠️ 完整异常打印到日志里，方便后端排查
        log.error("数据库异常: ", e);

        // ⚠️ 返回给前端的只能是模糊的提示，绝对不能带 e.getMessage()
        return ResponseEntity.status(ErrorCode.SQL_ERROR.getHttpStatus())
                .body(Result.error(ErrorCode.SQL_ERROR));
    }

    /**
     * 4. 处理全局未捕获的异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统未知异常: ", e);
        return ResponseEntity.status(ErrorCode.SYSTEM_ERROR.getHttpStatus())
                .body(Result.error(ErrorCode.SYSTEM_ERROR));
    }
}
