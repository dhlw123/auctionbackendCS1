package com.auction.bids.exceptions;

public class BuyItNowException extends RuntimeException {
    private String message;

    public BuyItNowException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
