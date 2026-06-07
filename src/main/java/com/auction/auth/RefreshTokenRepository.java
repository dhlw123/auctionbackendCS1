package com.auction.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository cung cấp các phương thức thao tác cơ sở dữ liệu với thực thể RefreshToken. */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

  /**
   * Tìm kiếm thông tin Refresh Token dựa trên chuỗi giá trị token.
   *
   * @param token Chuỗi Refresh Token cần tìm
   * @return Một Optional chứa thông tin RefreshToken nếu tìm thấy
   */
  @Query(value = "SELECT t FROM RefreshToken t WHERE t.refreshToken = :token")
  Optional<RefreshToken> findRefreshTokenData(@Param("token") String token);
}
