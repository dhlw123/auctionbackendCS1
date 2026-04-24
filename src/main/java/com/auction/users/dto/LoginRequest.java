package com.auction.users.dto;

import com.auction.common.annotations.NoSpace;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotEmpty(message="Username must not be empty")
        @NotNull(message="Username must not be null")
        @NoSpace(message = "Username can't have space")
        String username,
        @NotEmpty(message="Password must not be empty")
        @NotNull(message="Password must not be null")
        @NoSpace(message = "Password can't have space") String password) {
}