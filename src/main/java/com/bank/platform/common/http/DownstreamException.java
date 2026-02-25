package com.bank.platform.common.http;

public class DownstreamException extends RuntimeException {

    private final String code;
    private final int httpStatus;
    private final String target;
    private final String downstreamBody;

    public DownstreamException(String code, String message, int httpStatus, String target) {
        this(code, message, httpStatus, target, null);
    }

    public DownstreamException(String code, String message, int httpStatus, String target, String downstreamBody) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
        this.target = target;
        this.downstreamBody = downstreamBody;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getTarget() {
        return target;
    }

    public String getDownstreamBody() {
        return downstreamBody;
    }
}
