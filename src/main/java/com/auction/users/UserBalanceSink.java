package com.auction.users;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Thành phần quản lý luồng dữ liệu phản kháng (Reactive Stream) cho số dư của người dùng. Cho phép
 * phát và lắng nghe các thay đổi số dư theo thời gian thực (real-time).
 */
@Component
public class UserBalanceSink {

  // Bản đồ lưu trữ các sink phát dữ liệu số dư cho từng người dùng, sử dụng ConcurrentHashMap để
  // đảm bảo an toàn đa luồng (thread-safe)
  private final Map<String, Sinks.Many<Double>> balanceSinksMap;

  public UserBalanceSink() {
    this.balanceSinksMap = new ConcurrentHashMap<String, Sinks.Many<Double>>();
  }

  /**
   * Phát (emit) số dư mới của người dùng tới tất cả các luồng đang lắng nghe.
   *
   * @param username Tên đăng nhập người dùng
   * @param balance Số dư mới cần cập nhật
   */
  public void pushNewBalance(String username, Double balance) {
    Sinks.Many<Double> sink = balanceSinksMap.get(username);
    if (sink != null) {
      // Thử phát giá trị tiếp theo vào stream
      sink.tryEmitNext(balance);
    }
  }

  /**
   * Lấy hoặc khởi tạo luồng dữ liệu (Flux) chứa các cập nhật số dư của người dùng cụ thể.
   *
   * @param username Tên đăng nhập người dùng
   * @return Luồng Flux phát số dư (Double) khi có thay đổi
   */
  public Flux<Double> getBalanceSink(String username) {
    // Lấy sink hiện tại hoặc tạo mới nếu chưa tồn tại
    Sinks.Many<Double> sink =
        balanceSinksMap.computeIfAbsent(
            username,
            // Sử dụng multicast().onBackpressureBuffer() để hỗ trợ nhiều subscriber lắng nghe cùng
            // một luồng cập nhật số dư
            id -> Sinks.many().multicast().onBackpressureBuffer());

    // Trả về dưới dạng Flux và đăng ký giải phóng tài nguyên khi luồng kết thúc (doFinally)
    return sink.asFlux()
        .doFinally(
            signalType -> {
              balanceSinksMap.computeIfPresent(
                  username,
                  (id, existingSink) -> {
                    // Nếu không còn subscriber nào lắng nghe thì xóa sink này khỏi bản đồ để giải
                    // phóng bộ nhớ
                    if (existingSink.currentSubscriberCount() == 0) {
                      return null;
                    }
                    return existingSink;
                  });
            });
  }
}
