package com.auction.common;

public class BaseException extends RuntimeException {
    BaseResponse response;

    public BaseException(String message) {
        this.response = new BaseResponse(false, message);
    }
}
