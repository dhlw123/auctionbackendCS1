package com.auction.users;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents a refresh token entity in the database.
 * Each user has a unique refresh token associated with their username.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    private String username;

    private String refreshToken;

    protected RefreshToken() {
    }

    public RefreshToken(String username, String refreshToken) {
        this.username = username;
        this.refreshToken = refreshToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
