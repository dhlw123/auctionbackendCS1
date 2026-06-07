package com.auction.admin.dto;

import jakarta.validation.constraints.NotBlank;

/** DTO đại diện cho yêu cầu khóa tài khoản người dùng (Ban User). */
public record BanUserDto(
    // Tên đăng nhập của người dùng bị khóa, không được để trống
    @NotBlank String username) {}
