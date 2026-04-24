package com.auction.common;

import jakarta.validation.constraints.NotBlank;

public class BaseResponse {
    @NotBlank
    private boolean status;

    private String message;

    public BaseResponse() {
    }

    public BaseResponse(boolean status, String message) {
        this.status = status;
        this.message = message;
    }

    public boolean getStatus() {
        return status;
    }

    // Empty Constructor so that Jackson can create and use the set methods to
    // inject data in

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
