package com.auction.bids;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository cung cấp các phương thức truy xuất dữ liệu liên quan đến cấu hình tự động đặt giá
 * (AutoBid).
 */
@Repository
public interface AutoBidRepository extends JpaRepository<AutoBid, Long> {

  /**
   * Tìm kiếm cấu hình tự động đặt giá của một sản phẩm qua mã ID sản phẩm.
   *
   * @param itemId Mã ID sản phẩm
   * @return Một Optional chứa thông tin AutoBid nếu có cấu hình, ngược lại trả về rỗng
   */
  Optional<AutoBid> findByItemId(Long itemId);
}
