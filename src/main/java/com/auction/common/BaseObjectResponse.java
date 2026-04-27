package com.auction.common;

public class BaseObjectResponse<T> extends BaseResponse {
    private T entity;

    public BaseObjectResponse(boolean status, String message, T entity) {
        super(status, message);
        this.entity = entity;
    }

    public T getEntity() {
        return entity;
    }
}
