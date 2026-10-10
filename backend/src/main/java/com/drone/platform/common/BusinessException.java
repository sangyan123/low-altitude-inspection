package com.drone.platform.common;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
    public BusinessException(int code, String message) { super(message); }
}
