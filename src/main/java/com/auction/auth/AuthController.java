package com.auction.auth;

import com.auction.auth.dto.AuthResponse;
import com.auction.auth.dto.LoginRequest;
import com.auction.auth.dto.RefreshTokenRequest;
import com.auction.auth.dto.RegisterRequest;
import com.auction.auth.jwtools.UserDetailsImpl;
import com.auction.common.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller chịu trách nhiệm cung cấp các API liên quan đến Xác thực (Authentication) và Phân
 * quyền như Đăng ký, Đăng nhập, Đăng xuất và Làm mới Token.
 */
@RestController
@RequestMapping("")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * API làm mới Access Token bằng Refresh Token. POST /refresh
   *
   * @param request Yêu cầu chứa mã Refresh Token
   * @return ResponseEntity chứa cặp token mới (Access Token và Refresh Token)
   */
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    AuthResponse authResponse = authService.refreshingToken(request.refreshToken());
    return ResponseEntity.ok().body(authResponse);
  }

  /**
   * API đăng nhập người dùng vào hệ thống. POST /login
   *
   * @param request Yêu cầu đăng nhập chứa username và password
   * @return ResponseEntity chứa token xác thực nếu thông tin đăng nhập chính xác
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.loginUser(request);
    return ResponseEntity.ok().body(response);
  }

  /**
   * API đăng ký tài khoản người dùng mới. POST /register
   *
   * @param request Yêu cầu đăng ký chứa username, displayName và password
   * @return ResponseEntity phản hồi trạng thái đăng ký thành công
   */
  @PostMapping("/register")
  public ResponseEntity<BaseResponse> register(@Valid @RequestBody RegisterRequest request) {
    BaseResponse response = authService.userRegister(request);
    return ResponseEntity.ok().body(response);
  }

  /**
   * API đăng xuất tài khoản khỏi hệ thống, thu hồi Refresh Token hiện tại. POST /logout
   *
   * @param userDetailsImpl Thông tin người dùng hiện tại lấy từ Security Context
   * @return ResponseEntity phản hồi trạng thái đăng xuất thành công
   */
  @PostMapping("/logout")
  public ResponseEntity<BaseResponse> logout(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
    BaseResponse response = authService.logoutUser(userDetailsImpl);
    return ResponseEntity.ok(response);
  }
}
