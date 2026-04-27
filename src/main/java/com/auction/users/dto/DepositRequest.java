package com.auction.users.dto;

public class DepositRequest {
    Double amount;

    public DepositRequest(Double amount) {
        this.amount = amount;
    }

    public Double getAmount() {
        return amount;
    }

}
