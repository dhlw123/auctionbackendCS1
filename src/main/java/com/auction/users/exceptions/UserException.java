package com.auction.users.exceptions;

import com.auction.common.BaseResponse;

public class UserException extends RuntimeException {
    public BaseResponse response;

    public UserException(boolean status, String message) {
        response = new BaseResponse(status, message);
    }

    public BaseResponse getResponse() {
        return response;
    }
}
