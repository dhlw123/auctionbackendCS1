package com.auction.users;

import com.auction.users.dto.UserResponse;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String displayName;

    @JsonIgnore
    @Column(nullable = false)
    private String hashedPassword;
    private Double balance;

    User() {
    }

    public User(String username, String displayName, String hashedPassword, Double balance) {
        this.username = username;
        this.displayName = displayName;
        this.hashedPassword = hashedPassword;
        this.balance = balance;
    }

    public UserResponse toResponse() {
        return new UserResponse(getUsername(), getDisplayName(), getBalance());
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

}
