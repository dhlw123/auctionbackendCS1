package com.auction.auth.dto;

import com.auction.common.annotations.NoSpace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Record đại diện cho dữ liệu yêu cầu đăng ký tài khoản mới. */
public record RegisterRequest(
    // Tên tài khoản đăng ký (không trống, không null, không chứa khoảng trắng)
    @NotEmpty(message = "Username must not be empty")
        @NotNull(message = "Username must not be null")
        @NoSpace(message = "Username can't have space")
        String username,

    // Tên hiển thị công khai (không null, không để trống)
    @NotNull(message = "Display name must not be null")
        @NotBlank(message = "Display name can't be blank")
        String displayName,

    // Mật khẩu (không trống, không null, không chứa khoảng trắng)
    @NotEmpty(message = "Password must not be empty")
        @NotNull(message = "Password must not be null")
        @NoSpace(message = "Password can't have space")
        String password) {}
