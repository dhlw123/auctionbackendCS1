package com.auction.auth.jwtools;

import com.auction.users.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Lớp triển khai UserDetails của Spring Security. Dùng để lưu trữ thông tin tài khoản người dùng đã
 * được xác thực trong Spring Security Context.
 */
public class UserDetailsImpl implements UserDetails {
  private String username;
  private String displayName;
  private Double balance;
  private Collection<? extends GrantedAuthority> authorities;

  public UserDetailsImpl(
      String username,
      String displayName,
      Double balance,
      Collection<? extends GrantedAuthority> authorities) {
    this.username = username;
    this.displayName = displayName;
    this.balance = balance;
    this.authorities = authorities;
  }

  /**
   * Phương thức tĩnh hỗ trợ chuyển đổi từ đối tượng thực thể User (JPA) sang đối tượng
   * UserDetailsImpl. Cấp vai trò ROLE_ADMIN nếu tên đăng nhập là "admin".
   *
   * @param user Thực thể User cần chuyển đổi
   * @return Đối tượng UserDetailsImpl tương ứng
   */
  public static UserDetailsImpl JPAtoUserDetails(User user) {
    List<GrantedAuthority> authorities;
    if ("admin".equals(user.getUsername())) {
      authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    } else {
      authorities = List.of();
    }
    return new UserDetailsImpl(
        user.getUsername(), user.getDisplayName(), user.getBalance(), authorities);
  }

  @Override
  public String getUsername() {
    return username;
  }

  // Trả về mật khẩu rỗng vì cơ chế JWT không cần lưu trữ mật khẩu trong UserDetails sau khi đã xác
  // thực
  @Override
  public String getPassword() {
    return "";
  }

  public String getDisplayName() {
    return displayName;
  }

  public Double getBalance() {
    return balance;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }
}
