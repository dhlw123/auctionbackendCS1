package com.auction.bids;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.auction.items.Item;
import com.auction.users.User;

/**
 * Repository cung cấp các phương thức thao tác cơ sở dữ liệu với thực thể Bid.
 */
@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    /**
     * Tìm kiếm một lượt đặt giá cụ thể theo người dùng và mặt hàng.
     */
    Optional<Bid> findByUserAndItem(User user, Item item);

    /**
     * Tìm kiếm phân trang tất cả các lượt đặt giá của một người dùng.
     */
    Page<Bid> findAllByUser(User user, Pageable pageable);

    /**
     * Kiểm tra người dùng đã đặt giá cược cho sản phẩm này hay chưa.
     */
    boolean existsByUserAndItem(User user, Item item);

    /**
     * Kiểm tra xem sản phẩm này đã từng được ai đặt giá cược hay chưa.
     */
    boolean existsByItem(Item item);

    /**
     * Lấy phân trang lịch sử đặt cược của một sản phẩm cụ thể.
     * Sử dụng JPQL Custom Query kết hợp JOIN FETCH b.item để tránh lỗi N+1 query.
     *
     * @param pageable Cấu hình phân trang và sắp xếp
     * @param itemId   Mã ID sản phẩm
     * @return Danh sách phân trang các lượt cược
     */
    @Query(value = "SELECT b FROM Bid b JOIN FETCH b.item WHERE b.item.itemId = :itemId")
    public Page<Bid> findItemBidHistory(Pageable pageable, @Param("itemId") Long itemId);

    /**
     * Truy vấn JPQL tìm các lượt đấu giá chiến thắng của người dùng, kèm eager-load Item qua JOIN FETCH
     * để tránh N+1 query khi truy cập {@code bid.getItem()}.
     * Một người thắng cuộc khi:
     * - Họ đặt giá cho sản phẩm ({@code b.user.username = :username})
     * - Họ giữ giá cao nhất hiện tại ở bảng trạng thái ({@code s.highestBidUser = :username})
     * - Thời gian đấu giá của sản phẩm đó đã trôi qua ({@code s.endTime < :now})
     *
     * @param username Tên đăng nhập người dùng cần kiểm tra
     * @param now      Thời điểm hiện tại dưới dạng Epoch Milliseconds để đối chiếu thời gian kết thúc
     * @return Danh sách các lượt đặt giá giành chiến thắng (kèm Item đã được eager-load)
     */
    @Query("SELECT b FROM Bid b JOIN FETCH b.item JOIN ItemStatus s ON b.item = s.item WHERE b.user.username = :username AND s.highestBidUser = :username AND s.endTime < :now")
    List<Bid> getWinsByUser(@Param("username") String username, @Param("now") Long now);

    /**
     * Xóa các lượt đặt giá dựa trên thông tin sản phẩm và người dùng.
     */
    @Transactional
    Long deleteByItemAndUser(Item item, User user);
}
