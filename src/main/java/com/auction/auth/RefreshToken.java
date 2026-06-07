package com.auction.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Thực thể RefreshToken đại diện cho mã làm mới (Refresh Token) được sử dụng để gia hạn Access
 * Token. Được lưu vào cơ sở dữ liệu để kiểm soát phiên làm việc của người dùng.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

  // Tên đăng nhập của người dùng (Khóa chính), mỗi tài khoản chỉ có tối đa một Refresh Token tại
  // một thời điểm
  @Id
  @Column(name = "username")
  private String username;

  // Giá trị của mã Refresh Token mã hóa dạng JWT
  @Column(name = "token")
  private String refreshToken;

  // Thời gian tạo Refresh Token (Epoch Milliseconds)
  @Column(name = "created_at")
  private Long createdAt;

  /** Sự kiện Jpa Lifecycle Callback: Tự động ghi nhận thời điểm tạo Token trước khi lưu vào DB. */
  protected RefreshToken() {}

  public RefreshToken(String username, String refreshToken, Long createdAt) {
    this.username = username;
    this.refreshToken = refreshToken;
    this.createdAt = createdAt;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
  }
}
