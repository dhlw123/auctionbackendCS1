package com.auction.users;

import com.auction.auth.jwtools.UserDetailsImpl;
import com.auction.common.BaseObjectResponse;
import com.auction.users.dto.DepositRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp các API REST liên quan đến thông tin người dùng và tài chính cá nhân. Tất cả
 * các API yêu cầu xác thực JWT (Bearer Token).
 */
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * API nạp tiền vào ví của người dùng hiện tại đang đăng nhập. POST /users/me/deposit
   *
   * @param userDetailsImpl Thông tin người dùng đã xác thực lấy từ Spring Security Context
   * @param request Yêu cầu nạp tiền chứa số lượng tiền cần nạp
   * @return ResponseEntity chứa phản hồi số dư mới sau khi nạp thành công
   */
  @PostMapping("/me/deposit")
  public ResponseEntity<BaseObjectResponse<Double>> deposit(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
      @Valid @RequestBody DepositRequest request) {
    BaseObjectResponse<Double> response =
        userService.depositCredit(userDetailsImpl.getUsername(), request.amount());
    return ResponseEntity.ok().body(response);
  }

  /**
   * API lấy số dư tài khoản của người dùng hiện tại đang đăng nhập. GET /users/me/balance
   *
   * @param userDetailsImpl Thông tin người dùng đã xác thực
   * @return ResponseEntity chứa phản hồi số dư hiện tại
   */
  @GetMapping("/me/balance")
  public ResponseEntity<BaseObjectResponse<Double>> balance(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
    BaseObjectResponse<Double> response = userService.getBalance(userDetailsImpl.getUsername());
    return ResponseEntity.ok().body(response);
  }

  /**
   * API lấy toàn bộ thông tin cá nhân (Profile) của người dùng hiện tại đang đăng nhập. GET
   * /users/me
   *
   * @param userDetailsImpl Thông tin người dùng đã xác thực
   * @return ResponseEntity chứa thông tin chi tiết thực thể User
   */
  @GetMapping("/me")
  public ResponseEntity<User> profile(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
    User response = userService.getUserByUsername(userDetailsImpl.getUsername());
    return ResponseEntity.ok().body(response);
  }
}
