package com.enterprise.email.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseResult<T> {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    // 响应码常量
    public static final Integer SUCCESS = 200;
    public static final Integer ERROR = 500;
    public static final Integer UNAUTHORIZED = 401;
    public static final Integer FORBIDDEN = 403;
    public static final Integer NOT_FOUND = 404;
    public static final Integer BAD_REQUEST = 400;

    /**
     * 成功响应
     */
    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>(SUCCESS, "操作成功", data, System.currentTimeMillis());
    }

    /**
     * 成功响应（带消息）
     */
    public static <T> ResponseResult<T> success(T data, String message) {
        return new ResponseResult<>(SUCCESS, message, data, System.currentTimeMillis());
    }

    /**
     * 错误响应
     */
    public static <T> ResponseResult<T> error(String message) {
        return new ResponseResult<>(ERROR, message, null, System.currentTimeMillis());
    }

    /**
     * 错误响应（带响应码）
     */
    public static <T> ResponseResult<T> error(Integer code, String message) {
        return new ResponseResult<>(code, message, null, System.currentTimeMillis());
    }

    /**
     * 未授权响应
     */
    public static <T> ResponseResult<T> unauthorized(String message) {
        return new ResponseResult<>(UNAUTHORIZED, message, null, System.currentTimeMillis());
    }

    /**
     * 禁止访问响应
     */
    public static <T> ResponseResult<T> forbidden(String message) {
        return new ResponseResult<>(FORBIDDEN, message, null, System.currentTimeMillis());
    }

    /**
     * 资源不存在响应
     */
    public static <T> ResponseResult<T> notFound(String message) {
        return new ResponseResult<>(NOT_FOUND, message, null, System.currentTimeMillis());
    }

    /**
     * 请求参数错误响应
     */
    public static <T> ResponseResult<T> badRequest(String message) {
        return new ResponseResult<>(BAD_REQUEST, message, null, System.currentTimeMillis());
    }
}