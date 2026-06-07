package com.auction.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Thực thể RevokedToken lưu trữ thông tin các token đã bị thu hồi hoặc danh sách người dùng bị cấm
 * (banned). Dùng để kiểm tra tính hợp lệ của token JWT đã phát hành trước đó.
 */
@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

  // Tên đăng nhập của người dùng bị thu hồi quyền hoặc bị cấm (Khóa chính)
  @Id
  @Column(name = "username")
  private String username;

  // Thời điểm người dùng bị cấm hoặc thu hồi token (Epoch Milliseconds)
  @Column(name = "banned_at", nullable = false)
  private Long bannedAt;

  protected RevokedToken() {}

  public RevokedToken(String username, Long bannedAt) {
    this.username = username;
    this.bannedAt = bannedAt;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Long getBannedAt() {
    return bannedAt;
  }

  public void setBannedAt(Long bannedAt) {
    this.bannedAt = bannedAt;
  }
}
