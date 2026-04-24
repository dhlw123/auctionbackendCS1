package com.auction.items.exceptions;

import com.auction.common.BaseResponse;

public class ItemException extends RuntimeException {
    public BaseResponse response;

    public ItemException(boolean status, String message) {
        response = new BaseResponse(status, message);
    }

    public BaseResponse getResponse() {
        return response;
    }
}
