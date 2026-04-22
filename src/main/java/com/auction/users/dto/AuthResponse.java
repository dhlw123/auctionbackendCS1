package com.auction.users.dto;

public record AuthResponse(
        boolean success,
        String message,
        String token) {
}
