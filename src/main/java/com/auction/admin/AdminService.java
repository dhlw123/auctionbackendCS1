package com.auction.admin;

import com.auction.admin.dto.*;
import com.auction.auth.AuthService;
import com.auction.auth.RevokedToken;
import com.auction.auth.RevokedTokenRepository;
import com.auction.common.*;
import com.auction.items.ItemService;
import com.auction.users.User;
import com.auction.users.UserService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service quản trị viên (Admin Service). Cung cấp các công cụ quản trị hệ thống bao gồm: hủy phiên
 * đấu giá của sản phẩm, khóa tài khoản người dùng (Ban) thông qua thay thế mã băm mật khẩu và thu
 * hồi token, cũng như mở khóa tài khoản người dùng (Unban) bằng cách đặt lại mật khẩu mới.
 */
@Service
public class AdminService {

  private final ItemService itemService;
  private final UserService userService;
  private final AuthService authService;
  private final PasswordEncoder passwordEncoder;
  private final RevokedTokenRepository revokedTokenRepository;

  // Chuỗi mã băm dùng để thay thế mật khẩu của người dùng khi bị khóa tài khoản
  @Value("${ban_hash}")
  private String banHash;

  public AdminService(
      ItemService itemService,
      UserService userService,
      AuthService authService,
      PasswordEncoder passwordEncoder,
      RevokedTokenRepository revokedTokenRepository) {
    this.itemService = itemService;
    this.userService = userService;
    this.authService = authService;
    this.passwordEncoder = passwordEncoder;
    this.revokedTokenRepository = revokedTokenRepository;
  }

  /**
   * Hủy bỏ một phiên đấu giá sản phẩm theo yêu cầu của Admin.
   *
   * @param itemId Mã ID của sản phẩm đấu giá cần hủy
   * @return Phản hồi thông báo kết quả hủy thành công
   */
  @Transactional
  public BaseResponse cancelAuction(Long itemId) {
    // Lấy thông tin tên người bán từ sản phẩm để thực hiện luồng hủy cược/hoàn tiền
    String sellername = itemService.getItem(itemId).getUser().getUsername();
    return itemService.cancelItem(itemId, sellername);
  }

  /**
   * Khóa tài khoản của một người dùng (Ban User). Không cho phép khóa tài khoản quản trị hệ thống
   * "admin". Quy trình khóa bao gồm thu hồi token hiện tại, ghi đè mật khẩu bằng mã băm banHash và
   * thêm vào danh sách đen.
   *
   * @param username Tên đăng nhập của người dùng cần khóa
   * @return Phản hồi thông báo khóa tài khoản thành công
   */
  @Transactional
  public BaseResponse banUser(String username) {
    // Ngăn chặn hành vi khóa tài khoản admin
    if (username.equals("admin")) {
      throw new BaseException("You can't ban admin");
    }
    User user = userService.getUserByUsername(username);

    // Thu hồi các token đăng nhập hiện có của người dùng
    authService.revokeToken(username);

    // Thiết lập mật khẩu của người dùng thành mã băm vô hiệu (banHash) để ngăn không cho đăng nhập
    // lại
    user.setHashedPassword(banHash);
    userService.saveUser(user);

    // Lưu thông tin thu hồi token vào DB với thời gian hiện tại
    revokedTokenRepository.save(new RevokedToken(username, Instant.now().toEpochMilli()));

    return new BaseResponse(true, "successfully banned user");
  }

  /**
   * Mở khóa tài khoản của người dùng (Unban User). Thiết lập lại mật khẩu mới được chỉ định và gỡ
   * bỏ người dùng khỏi danh sách đen token bị thu hồi.
   *
   * @param request Yêu cầu chứa username cần mở khóa và mật khẩu mới
   * @return Phản hồi thông báo mở khóa tài khoản thành công
   */
  @Transactional
  public BaseResponse unbanUser(UnbanRequest request) {
    User user = userService.getUserByUsername(request.username());

    // Mã hóa mật khẩu mới và thay thế mã băm banHash cũ
    String hashedPassword = passwordEncoder.encode(request.password());
    user.setHashedPassword(hashedPassword);
    userService.saveUser(user);

    // Xóa thông tin tài khoản khỏi danh sách đen thu hồi token để cho phép đăng nhập lại
    revokedTokenRepository.deleteById(request.username());

    return new BaseResponse(true, "Succesfully unbanned user.");
  }
}
