package com.auction.security;

import com.auction.users.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserDetailsImpl implements UserDetails {
    private String username;
    private String displayName;
    private Double balance;
    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(String username, String displayName, Double balance,
                           Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.displayName = displayName;
        this.balance = balance;
        this.authorities = authorities;
    }

    public static UserDetailsImpl JPAtoUserDetails(User user) {
        return new UserDetailsImpl(user.getUsername(), user.getDisplayName(), user.getBalance(),
                Collections.emptyList());
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return "";
    }

    public String getDisplayName() {
        return displayName;
    }

    public Double getBalance() {
        return balance;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
