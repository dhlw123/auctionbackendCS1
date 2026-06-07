package com.auction.items;

import com.auction.users.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository quản lý việc truy vấn dữ liệu từ bảng "items". */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

  /**
   * Tìm kiếm phân trang các sản phẩm hiện đang mở đấu giá (chưa đến thời gian kết thúc). Truy vấn
   * sử dụng JPQL tham chiếu bảng ItemStatus.
   *
   * @param pageable Thông tin phân trang (trang hiện tại, kích thước trang, sắp xếp)
   * @param currentTime Thời gian hiện tại dưới dạng Epoch Milliseconds để so sánh với thời gian kết
   *     thúc
   * @return Danh sách phân trang các sản phẩm đang hoạt động
   */
  @Query(
      value = "SELECT itemstat.item FROM ItemStatus itemstat WHERE itemstat.endTime > :currentTime")
  public Page<Item> findActiveItemPage(Pageable pageable, @Param("currentTime") Long currentTime);

  /**
   * Truy vấn phân trang toàn bộ sản phẩm đăng đấu giá của một người dùng (người bán) cụ thể.
   *
   * @param pageable Thông tin phân trang
   * @param user Đối tượng người bán
   * @return Danh sách phân trang các sản phẩm của người dùng
   */
  public Page<Item> findItemByUser(Pageable pageable, User user);
}
