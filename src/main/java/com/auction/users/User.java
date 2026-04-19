package com.auction.users;

public class User {
    private String userName;
    private String displayName;
    private String passWord;
    public String getUserName() {
        return userName;
    }
    public String getDisplayName() {
        return displayName;
    }

    public void updateDisplayName(String name) {
        displayName = name;
    }
}
