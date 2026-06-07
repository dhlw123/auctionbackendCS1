package com.auction.admin;

import com.auction.admin.dto.*;
import com.auction.admin.dto.BanUserDto;
import com.auction.auth.jwtools.UserDetailsImpl;
import com.auction.common.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller cung cấp các API dành riêng cho quản trị viên (Admin). Quản lý các hoạt động kiểm
 * duyệt hệ thống: khóa người dùng (ban), mở khóa người dùng (unban) và hủy bỏ phiên đấu giá sản
 * phẩm vi phạm hoặc gặp sự cố.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

  private final AdminService adminService;

  public AdminController(AdminService adminService) {
    this.adminService = adminService;
  }

  /**
   * API khóa tài khoản người dùng (Ban User). POST /admin/ban
   *
   * @param banDetails Thông tin tên đăng nhập của người dùng cần khóa
   * @param userDetailsImpl Thông tin tài khoản Admin đang thực hiện cuộc gọi API này
   * @return ResponseEntity phản hồi trạng thái khóa tài khoản thành công
   */
  @PostMapping("/ban")
  public ResponseEntity<BaseResponse> ban(
      @Valid @RequestBody BanUserDto banDetails,
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
    BaseResponse response = adminService.banUser(banDetails.username());
    return ResponseEntity.ok(response);
  }

  /**
   * API hủy bỏ một phiên đấu giá sản phẩm đang diễn ra. POST /admin/cancel/{itemId}
   *
   * @param itemId Mã ID của sản phẩm đấu giá cần hủy
   * @return ResponseEntity phản hồi trạng thái hủy phiên đấu giá thành công
   */
  @PostMapping("/cancel/{itemId}")
  public ResponseEntity<BaseResponse> cancelItem(@PathVariable Long itemId) {
    BaseResponse response = adminService.cancelAuction(itemId);
    return ResponseEntity.ok(response);
  }

  /**
   * API mở khóa tài khoản người dùng (Unban User) và thiết lập mật khẩu mới. POST /admin/unban
   *
   * @param request Yêu cầu mở khóa chứa tên đăng nhập và mật khẩu mới để phục hồi tài khoản
   * @return ResponseEntity phản hồi trạng thái mở khóa thành công
   */
  @PostMapping("/unban")
  public ResponseEntity<BaseResponse> unban(@Valid @RequestBody UnbanRequest request) {
    BaseResponse response = adminService.unbanUser(request);
    return ResponseEntity.ok().body(response);
  }

  @PostMapping("/finalize")
  public ResponseEntity<BaseResponse> finalizeExpired() {
    BaseResponse response = adminService.finalizeExpiredAuctions();
    return ResponseEntity.ok(response);
  }
}
