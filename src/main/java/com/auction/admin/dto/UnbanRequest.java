package com.auction.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO đại diện cho yêu cầu mở khóa tài khoản người dùng (Unban User). Yêu cầu này đi kèm mật khẩu
 * mới để thiết lập lại cho tài khoản sau khi mở khóa.
 */
public record UnbanRequest(
    // Tên đăng nhập của người dùng cần mở khóa, không được để trống
    @NotBlank String username,

    // Mật khẩu mới thiết lập cho tài khoản sau khi được mở khóa, không được để trống
    @NotBlank String password) {}
