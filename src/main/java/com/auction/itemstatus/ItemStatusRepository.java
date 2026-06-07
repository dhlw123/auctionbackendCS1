package com.auction.itemstatus;

import com.auction.items.Item;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository cung cấp các phương thức truy vấn và khóa bản ghi cho bảng "item_statuses". */
public interface ItemStatusRepository extends JpaRepository<ItemStatus, Long> {

  /**
   * Tìm trạng thái sản phẩm bằng thực thể Item và áp dụng khóa ghi bi quan (Pessimistic Write
   * Lock). Ngăn chặn các luồng (thread) khác sửa đổi bản ghi này cùng lúc, đảm bảo an toàn luồng
   * khi đặt giá cược.
   *
   * @param item Thực thể sản phẩm cần khóa và truy vấn
   * @return Trạng thái sản phẩm được khóa ghi
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(value = "SELECT s FROM ItemStatus s WHERE s.item = :item")
  ItemStatus findByItemWithLock(@Param("item") Item item);

  /**
   * Tìm trạng thái sản phẩm bằng ID sản phẩm và áp dụng khóa ghi bi quan (Pessimistic Write Lock).
   *
   * @param itemId Mã ID sản phẩm
   * @return Trạng thái sản phẩm được khóa ghi
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(value = "SELECT s FROM ItemStatus s WHERE s.item.itemId = :itemId")
  ItemStatus findByItemWithLockByItemId(@Param("itemId") Long itemId);

  List<ItemStatus> findByItemStatusAndEndTimeBefore(String itemStatus, Long endTime);
}
