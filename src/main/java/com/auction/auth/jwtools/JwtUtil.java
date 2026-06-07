package com.auction.auth.jwtools;

import com.auction.auth.exceptions.JwtExpiredException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Lớp tiện ích quản lý và xử lý mã thông báo JWT (JSON Web Token). Hỗ trợ tạo Access Token, Refresh
 * Token, trích xuất thông tin claims và kiểm tra tính hợp lệ của token.
 */
@Component
public class JwtUtil {
  private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

  // Chuỗi mã hóa bí mật JWT (cấu hình trong application.properties)
  @Value("${jwt.secret}")
  private String jwtSecret;

  // Thời gian sống của Access Token (ms)
  @Value("${jwt.expiration}")
  private int jwtExpirationms;

  // Thời gian sống của Refresh Token (ms)
  @Value("${jwt.refreshExpiration}")
  private int jwtRefreshExpirationms;

  private SecretKey key;

  /** Khởi tạo đối tượng SecretKey hmacShaKeyFor dựa trên chuỗi khóa bí mật jwtSecret. */
  @PostConstruct
  public void init() {
    this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  /** Tạo mới mã Access Token sử dụng tên tài khoản (username) làm Subject. */
  public String generateToken(String username) {
    return Jwts.builder()
        .subject(username)
        .issuedAt(new Date())
        .expiration(new Date((new Date().getTime()) + jwtExpirationms))
        .signWith(key)
        .compact();
  }

  /** Tạo mới mã Refresh Token sử dụng tên tài khoản (username) làm Subject. */
  public String generateRefreshToken(String username) {
    return Jwts.builder()
        .subject(username)
        .issuedAt(new Date())
        .expiration(new Date((new Date().getTime()) + jwtRefreshExpirationms))
        .signWith(key)
        .compact();
  }

  /** Trích xuất thông tin người dùng (Subject) lưu trong token JWT. */
  public String getUserFromToken(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
  }

  /** Trích xuất thời điểm phát hành (IssuedAt) của token JWT. */
  public Date getIssuedAtFromToken(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getIssuedAt();
  }

  /**
   * Kiểm tra tính hợp lệ và chữ ký của token JWT.
   *
   * @param token Chuỗi token JWT cần kiểm tra
   * @return true nếu token hợp lệ và còn hạn
   * @throws JwtExpiredException nếu token hết hạn hoặc có chữ ký không hợp lệ
   */
  public boolean validateJwtToken(String token) throws JwtExpiredException {
    try {
      Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      log.warn("JWT validation error: {}", e.getMessage());
      throw new JwtExpiredException();
    }
  }
}
