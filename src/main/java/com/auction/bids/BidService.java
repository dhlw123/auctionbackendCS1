package com.auction.bids;

import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;
import com.auction.items.Item;
import com.auction.users.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service quản lý các nghiệp vụ liên quan đến lượt đặt giá (Bid) và tự động đặt giá (Auto-Bid). */
@Service
public class BidService {

  private final BidRepository bidRepository;
  private final AutoBidRepository autoBidRepository;

  public BidService(BidRepository bidRepository, AutoBidRepository autoBidRepository) {
    this.bidRepository = bidRepository;
    this.autoBidRepository = autoBidRepository;
  }

  /**
   * Lấy phân trang toàn bộ danh sách đặt giá của một mặt hàng cụ thể, sắp xếp theo số tiền cược
   * tăng dần.
   */
  @Transactional(readOnly = true)
  public BaseObjectResponse<Page<Bid>> getBidsOnItem(Long itemId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("bidAmount"));
    Page<Bid> items = bidRepository.findItemBidHistory(pageable, itemId);
    return new BaseObjectResponse<Page<Bid>>(true, "Succesfully get all bids", items);
  }

  /** Kiểm tra xem người dùng đã thực hiện đặt giá trên mặt hàng cụ thể này hay chưa. */
  @Transactional(readOnly = true)
  public boolean existUserAndItem(User user, Item item) {
    return bidRepository.existsByUserAndItem(user, item);
  }

  /** Lưu thông tin lượt đặt giá mới vào cơ sở dữ liệu. */
  @Transactional
  public Bid saveBid(Bid bid) {
    bid = bidRepository.save(bid);
    return bid;
  }

  /**
   * Lấy chi tiết thông tin lượt đặt giá của một người dùng trên một mặt hàng cụ thể. Ném ngoại lệ
   * nếu không tìm thấy.
   */
  @Transactional
  public Bid getBidByUserAndItem(User user, Item item) {
    Bid bid =
        bidRepository
            .findByUserAndItem(user, item)
            .orElseThrow(() -> new BaseException("Unable to find user or item"));
    return bid;
  }

  /** Lấy toàn bộ danh sách các lượt đặt giá mà người dùng đã thực hiện. */
  @Transactional(readOnly = true)
  public Page<Bid> getAllUserBid(User userRef, Pageable pageable) {
    Page<Bid> bids = bidRepository.findAllByUser(userRef, pageable);
    return bids;
  }

  /**
   * Lấy danh sách các phiên đấu giá mà người dùng đã thắng cuộc ở thời điểm hiện tại.
   *
   * @param username Tên đăng nhập người dùng
   * @return Danh sách các lượt đặt giá đem lại thắng cuộc
   */
  @Transactional
  public List<Bid> getUserWins(String username) {
    List<Bid> bids = bidRepository.getWinsByUser(username, Instant.now().toEpochMilli());
    return bids;
  }

  /** Tìm kiếm cấu hình tự động đặt giá (Auto-Bid) của một mặt hàng cụ thể. */
  @Transactional(readOnly = true)
  public Optional<AutoBid> getAutoBidByItemId(Long itemId) {
    return autoBidRepository.findByItemId(itemId);
  }

  /** Tạo mới hoặc cập nhật cấu hình tự động đặt giá (Auto-Bid). */
  @Transactional
  public void saveAutoBid(AutoBid autoBid) {
    autoBidRepository.save(autoBid);
  }

  /** Xóa bỏ cấu hình tự động đặt giá (Auto-Bid). */
  @Transactional
  public void deleteAutoBid(AutoBid autoBid) {
    autoBidRepository.delete(autoBid);
  }
}
