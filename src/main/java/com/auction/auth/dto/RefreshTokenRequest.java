package com.auction.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Record đại diện cho dữ liệu yêu cầu gia hạn Access Token bằng Refresh Token. */
public record RefreshTokenRequest(
    // Chuỗi Refresh Token hiện có của người dùng, không được để trống
    @NotBlank String refreshToken) {}
