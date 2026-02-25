package com.bank.platform.common.error;

import java.util.Map;

public class ForbiddenBusinessException extends RuntimeException {

    private final String code;
    private final Map<String, Object> details;

    public ForbiddenBusinessException(String code, String message) {
        this(code, message, null);
    }

    public ForbiddenBusinessException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}