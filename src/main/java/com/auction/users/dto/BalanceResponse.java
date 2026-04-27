package com.auction.users.dto;

import com.auction.common.BaseResponse;

public class BalanceResponse extends BaseResponse {
    Double balance;

    public BalanceResponse(boolean status, String messsage, Double balance) {
        super(status, messsage);
        this.balance = balance;
    }

    public Double getBalance() {
        return balance;
    }
}
