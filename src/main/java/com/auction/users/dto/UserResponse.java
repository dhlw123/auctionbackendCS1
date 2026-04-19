package com.auction.users.dto;

import java.util.UUID;

import com.auction.users.User;


public class UserResponse {
    private String username;
    private String displayName;
    private Double balance;
    
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
