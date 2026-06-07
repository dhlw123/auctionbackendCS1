package com.auction.users;

import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service quản lý các nghiệp vụ (Business Logic) liên quan đến người dùng, bao gồm quản lý thông
 * tin tài khoản và cập nhật số dư ví.
 */
@Service
public class UserService {

  private final UserRepository userRepository;
  private final UserBalanceSink userBalanceSink;

  public UserService(UserRepository userRepository, UserBalanceSink userBalanceSink) {
    this.userRepository = userRepository;
    this.userBalanceSink = userBalanceSink;
  }

  /**
   * Lấy tham chiếu (Proxy Reference) của thực thể User theo tên đăng nhập. Sử dụng getReferenceById
   * giúp tránh truy vấn cơ sở dữ liệu nếu chỉ cần thiết lập quan hệ khóa ngoại (Foreign Key).
   *
   * @param username Tên đăng nhập người dùng
   * @return Đối tượng tham chiếu của User
   */
  @Transactional
  public User getUserReferenceByUsername(String username) {
    User userRef = userRepository.getReferenceById(username);
    return userRef;
  }

  /**
   * Nạp thêm tiền vào tài khoản người dùng và cập nhật số dư mới qua Reactive Stream.
   *
   * @param username Tên đăng nhập người dùng
   * @param amount Số tiền cần nạp
   * @return Số dư tài khoản mới sau khi nạp tiền
   */
  @Transactional
  public Double addBalance(String username, Double amount) {
    User user = getUserByUsername(username);
    user.addBalance(amount);
    userRepository.save(user);

    // Phát số dư mới tới luồng phản kháng cho các kết nối real-time
    userBalanceSink.pushNewBalance(username, user.getBalance());
    return user.getBalance();
  }

  /**
   * Trừ tiền trong tài khoản người dùng (khi thực hiện bid hoặc thanh toán) và cập nhật số dư mới
   * qua Reactive Stream.
   *
   * @param username Tên đăng nhập người dùng
   * @param amount Số tiền cần trừ
   */
  @Transactional
  public void deductBalance(String username, Double amount) {
    User user = getUserByUsername(username);
    user.deductBalance(amount);
    userRepository.save(user);

    // Phát số dư mới tới luồng phản kháng cho các kết nối real-time
    userBalanceSink.pushNewBalance(username, user.getBalance());
  }

  /**
   * Thực hiện nghiệp vụ nạp tiền và đóng gói dữ liệu phản hồi dạng BaseObjectResponse.
   *
   * @param username Tên đăng nhập người dùng
   * @param creditAmount Số tiền cần nạp
   * @return BaseObjectResponse chứa trạng thái thành công và số dư mới
   */
  @Transactional
  public BaseObjectResponse<Double> depositCredit(String username, Double creditAmount) {
    Double newBalance = addBalance(username, creditAmount);
    return new BaseObjectResponse<>(
        true, "Succesfully deposited credit, current balance", newBalance);
  }

  /**
   * Truy vấn số dư ví của người dùng.
   *
   * @param username Tên đăng nhập người dùng
   * @return BaseObjectResponse chứa số dư ví
   */
  @Transactional(readOnly = true)
  public BaseObjectResponse<Double> getBalance(String username) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new BaseException("Invalid username"));
    return new BaseObjectResponse<>(true, "Get balance successful", user.getBalance());
  }

  /**
   * Tìm kiếm thông tin thực thể User theo tên đăng nhập. Ném lỗi nếu không tồn tại.
   *
   * @param username Tên đăng nhập cần tìm
   * @return Thực thể User tương ứng
   * @throws BaseException nếu người dùng không tồn tại trong hệ thống
   */
  @Transactional(readOnly = true)
  public User getUserByUsername(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new BaseException("User not found"));
  }

  /**
   * Kiểm tra xem tên đăng nhập đã tồn tại trên hệ thống chưa.
   *
   * @param username Tên đăng nhập cần kiểm tra
   * @return true nếu đã tồn tại, ngược lại trả về false
   */
  @Transactional(readOnly = true)
  public boolean existsUsername(String username) {
    boolean response = userRepository.existsByUsername(username);
    return response;
  }

  /**
   * Lưu thông tin thực thể User vào cơ sở dữ liệu.
   *
   * @param user Thực thể User cần lưu hoặc cập nhật
   * @return Thực thể User sau khi lưu
   */
  @Transactional
  public User saveUser(User user) {
    user = userRepository.save(user);
    return user;
  }

  /**
   * Lấy tham chiếu (Proxy Reference) tương tự phương thức getUserReferenceByUsername.
   *
   * @param username Tên đăng nhập người dùng
   * @return Đối tượng tham chiếu của User
   */
  @Transactional
  public User getUserRef(String username) {
    User userRef = userRepository.getReferenceById(username);
    return userRef;
  }
}
