package com.auction.users;

import com.auction.users.dto.UserResponse;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Thực thể User đại diện cho thông tin tài khoản người dùng trong cơ sở dữ liệu. */
@Entity
@Table(name = "users")
public class User {
  private static final Logger log = LoggerFactory.getLogger(User.class);

  // Tên đăng nhập của người dùng, đóng vai trò là khóa chính (Primary Key)
  @Id
  @Column(unique = true, nullable = false)
  private String username;

  // Tên hiển thị công khai của người dùng
  @Column(nullable = false)
  private String displayName;

  // Mật khẩu đã được mã hóa, sử dụng @JsonIgnore để tránh lộ mật khẩu khi trả về JSON cho client
  @JsonIgnore
  @Column(nullable = false)
  private String hashedPassword;

  // Số dư ví của người dùng dùng để đấu giá
  private Double balance;

  // Constructor mặc định bắt buộc đối với JPA Entity
  public User() {}

  // Constructor đầy đủ tham số
  public User(String username, String displayName, String hashedPassword, Double balance) {
    this.username = username;
    this.displayName = displayName;
    this.hashedPassword = hashedPassword;
    this.balance = balance;
  }

  /** Chuyển đổi thông tin thực thể User sang UserResponse DTO để gửi lại cho Client. */
  public UserResponse toResponse() {
    return new UserResponse(getUsername(), getDisplayName(), getBalance());
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getHashedPassword() {
    return hashedPassword;
  }

  public void setHashedPassword(String hashedPassword) {
    this.hashedPassword = hashedPassword;
  }

  public Double getBalance() {
    return balance;
  }

  public void setBalance(Double balance) {
    this.balance = balance;
  }

  /**
   * Nạp thêm tiền vào tài khoản người dùng.
   *
   * @param value Số tiền cần nạp (phải lớn hơn hoặc bằng 0)
   */
  public void addBalance(Double value) {
    if (value < 0) {
      log.warn("Must not add negative value");
      return;
    }
    this.balance += value;
  }

  /**
   * Trừ tiền trong tài khoản người dùng (khi đặt cược hoặc thắng đấu giá).
   *
   * @param value Số tiền cần trừ (phải lớn hơn hoặc bằng 0)
   */
  public void deductBalance(Double value) {
    if (value < 0) {
      log.warn("Must not deduct negative value");
      return;
    }
    this.balance -= value;
  }
}
