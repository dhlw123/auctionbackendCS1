package com.auction.auth.dto;

import com.auction.common.BaseResponse;

/** Lớp phản hồi thông tin xác thực sau khi đăng nhập hoặc làm mới token thành công. */
public class AuthResponse extends BaseResponse {
  // Mã Access Token dùng để truy cập các tài nguyên bảo mật (thường có thời gian sống ngắn)
  private String accessToken;

  // Mã Refresh Token dùng để gia hạn Access Token mới (thường có thời gian sống dài hơn)
  private String refreshToken;

  public AuthResponse(boolean status, String message, String accessToken, String refreshToken) {
    super(status, message);
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
