package com.auction.auth.dto;

import com.auction.common.annotations.NoSpace;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Record đại diện cho dữ liệu yêu cầu đăng nhập của người dùng. */
public record LoginRequest(
    // Tên tài khoản đăng nhập (không trống, không null, không chứa khoảng trắng)
    @NotEmpty(message = "Username must not be empty")
        @NotNull(message = "Username must not be null")
        @NoSpace(message = "Username can't have space")
        String username,

    // Mật khẩu đăng nhập (không trống, không null, không chứa khoảng trắng)
    @NotEmpty(message = "Password must not be empty")
        @NotNull(message = "Password must not be null")
        @NoSpace(message = "Password can't have space")
        String password) {}
