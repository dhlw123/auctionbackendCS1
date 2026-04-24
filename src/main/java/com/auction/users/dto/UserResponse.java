package com.auction.users.dto;

public class UserResponse {
    private final String username;
    private final String displayName;
    private final Double balance;

    public UserResponse(String username, String displayname, double balance) {
        this.username = username;
        this.displayName = displayname;
        this.balance = balance;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;

    }

    public double getBalance() {
        return balance;
    }

}
