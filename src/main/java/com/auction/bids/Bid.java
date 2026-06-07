package com.auction.bids;

import com.auction.items.Item;
import com.auction.users.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Thực thể Bid đại diện cho một lượt đặt giá thủ công của người dùng đối với một sản phẩm đấu giá.
 */
@Entity
@Table(name = "bids")
public class Bid {

  // Mã ID lượt đặt giá (Khóa chính), tự động tăng
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "bid_id")
  private Long bidId;

  // Sản phẩm đấu giá liên kết. Một sản phẩm có thể có nhiều lượt đặt giá.
  @JsonIgnore
  @ManyToOne
  @JoinColumn(name = "item_id")
  private Item item;

  // Người thực hiện đặt giá. Một người dùng có thể thực hiện nhiều lượt đặt giá.
  @JsonIgnore
  @ManyToOne
  @JoinColumn(name = "bidder_username")
  private User user;

  // Số tiền đặt cược cho lượt đấu giá này
  @Column(name = "bid_amount")
  private Double bidAmount;

  // Thời gian đặt giá (Epoch Milliseconds)
  @Column(name = "bid_time")
  private Long time;

  /**
   * Sự kiện Jpa Lifecycle Callback: Tự động ghi lại thời gian thực hiện đặt giá trước khi lưu vào
   * cơ sở dữ liệu.
   */
  @PrePersist
  void addTime() {
    time = Instant.now().toEpochMilli();
  }

  public Bid() {}
  ;

  public Bid(Item item, User bidder_username, Double bidAmount) {
    this.item = item;
    this.user = bidder_username;
    this.bidAmount = bidAmount;
  }

  public Long getBidId() {
    return bidId;
  }

  public void setBidId(Long bidId) {
    this.bidId = bidId;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Double getBidAmount() {
    return bidAmount;
  }

  public void setBidAmount(Double bidAmount) {
    this.bidAmount = bidAmount;
  }

  public Long getTime() {
    return time;
  }

  public void setTime(Long time) {
    this.time = time;
  }
}
