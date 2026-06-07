package com.auction.bids;

import com.auction.users.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Thực thể AutoBid đại diện cho cấu hình tự động đặt giá (Auto-Bid) của một người dùng đối với một
 * sản phẩm đấu giá. Hệ thống sẽ tự động nâng giá cược khi có người cược cao hơn, cho tới khi chạm
 * giới hạn maxBidLimit.
 */
@Entity
@Table(name = "autobids")
public class AutoBid {

  // Mã ID sản phẩm đấu giá (Khóa chính), mỗi sản phẩm tại một thời điểm chỉ có tối đa một cấu hình
  // AutoBid hoạt động
  @Id
  @JsonIgnore
  @Column(name = "item_id")
  private Long itemId;

  // Giới hạn giá cược tối đa mà người dùng sẵn sàng chi trả cho sản phẩm này
  @Column(name = "max_bid_limit")
  private Double maxBidLimit;

  // Giá trị cược hiện tại mà hệ thống tự động đã đặt thay cho người dùng
  @Column(name = "current_bid_value")
  private Double currentBidValue;

  // Người dùng thiết lập cấu hình tự động đặt giá này
  @JsonIgnore
  @ManyToOne
  @JoinColumn(name = "bidder_username")
  private User bidder;

  // Thời gian cấu hình hoặc cập nhật tự động đặt giá
  @Column(name = "bid_time")
  private Long time;

  /** Sự kiện Jpa Lifecycle Callback: Tự động ghi lại thời gian tạo cấu hình AutoBid. */
  @PrePersist
  void addTime() {
    time = Instant.now().toEpochMilli();
  }

  public AutoBid() {}
  ;

  public AutoBid(Long itemId, User bidder, Double maxBidLimit, Double currentBidValue) {
    this.itemId = itemId;
    this.maxBidLimit = maxBidLimit;
    this.currentBidValue = currentBidValue;
    this.bidder = bidder;
  }

  public Double getCurrentBidValue() {
    return currentBidValue;
  }

  public void setCurrentBidValue(Double currentBidValue) {
    this.currentBidValue = currentBidValue;
  }

  public Long getItemId() {
    return itemId;
  }

  public Double getMaxBidLimit() {
    return maxBidLimit;
  }

  public User getBidder() {
    return bidder;
  }

  public Long getTime() {
    return time;
  }

  public void setMaxBidLimit(Double maxBidLimit) {
    this.maxBidLimit = maxBidLimit;
  }
}
