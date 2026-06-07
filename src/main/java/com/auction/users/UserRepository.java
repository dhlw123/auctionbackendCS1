package com.auction.users;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository cung cấp các phương thức truy vấn dữ liệu từ bảng "users" trong cơ sở dữ liệu. */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

  /**
   * Tìm kiếm một người dùng theo tên đăng nhập (username).
   *
   * @param username Tên đăng nhập cần tìm kiếm
   * @return Một Optional chứa User nếu tìm thấy, ngược lại trả về Optional rỗng
   */
  Optional<User> findByUsername(String username);

  /**
   * Kiểm tra sự tồn tại của tên đăng nhập (username) trong cơ sở dữ liệu.
   *
   * @param username Tên đăng nhập cần kiểm tra
   * @return true nếu tên đăng nhập đã tồn tại, ngược lại trả về false
   */
  boolean existsByUsername(String username);
}
