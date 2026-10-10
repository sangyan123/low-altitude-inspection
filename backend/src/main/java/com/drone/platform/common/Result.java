package com.drone.platform.common;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok() { return build(200, "success", null); }
    public static <T> Result<T> ok(T data) { return build(200, "success", data); }
    public static <T> Result<T> fail(String message) { return build(500, message, null); }
    public static <T> Result<T> fail(int code, String message) { return build(code, message, null); }
    public static <T> Result<T> unauthorized() { return build(401, "未登录或令牌已过期", null); }
    public static <T> Result<T> forbidden() { return build(403, "无权限访问", null); }

    private static <T> Result<T> build(int code, String message, T data) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        r.setData(data);
        return r;
    }
}
