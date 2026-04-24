package com.auction.users;

import com.auction.users.dto.UserResponse;
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

    public Double getBalance() {
        return balance;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void updateDisplayName(String name) {
        displayName = name;
    }

    public UserResponse toResponse() {
        return new UserResponse(getUsername(), getDisplayName(), getBalance());
    }
}
