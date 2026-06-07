package com.auction.itemstatus;

import com.auction.items.Item;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Thực thể ItemStatus đại diện cho trạng thái hiện tại và các cấu hình đấu giá của một sản phẩm.
 * Lưu trữ giá hiện tại, người đặt cược cao nhất, thời hạn và trạng thái (ACTIVE, CANCELED, ENDED,
 * v.v.).
 */
@Entity
@Table(name = "item_statuses")
public class ItemStatus {

  // Khóa chính của bảng trạng thái sản phẩm, sẽ khớp hoàn toàn với khóa chính của thực thể Item
  // (item_id)
  @Id private Long id;

  // Mặt hàng liên kết. Thiết lập quan hệ One-to-One và sử dụng MapsId để dùng chung ID với Item.
  @JsonIgnore
  @OneToOne
  @MapsId
  @JoinColumn(name = "item_id")
  private Item item;

  // Giá đặt cược cao nhất hiện tại
  @Column(name = "current_price")
  private Double currentPrice;

  // Tên đăng nhập của người đang trả giá cao nhất
  @Column(name = "username")
  private String highestBidUser;

  // Thời gian bắt đầu đấu giá (Epoch Milliseconds)
  @Column(name = "start_time")
  private Long startTime;

  // Thời gian dự kiến kết thúc đấu giá (Epoch Milliseconds)
  @Column(name = "end_time")
  private Long endTime;

  // Thời gian kết thúc tối đa cho phép kể cả khi có bù giờ (Epoch Milliseconds)
  @Column(name = "max_end_time")
  private Long maxEndTime;

  // Giá khởi điểm ban đầu của sản phẩm
  @Column(name = "starting_price")
  private Double startingPrice;

  // Giá mua đứt để thắng cuộc ngay lập tức
  @Column(name = "buy_it_now_price")
  private Double buyItNowPrice;

  // Bước giá tăng tối thiểu cho lượt đấu giá tiếp theo
  @Column(name = "bid_increment")
  private Double bidIncrement;

  // Trạng thái của mặt hàng đấu giá (Ví dụ: ACTIVE, CANCELED, ENDED, SOLD)
  @Column(name = "item_status")
  private String itemStatus;

  /**
   * Sự kiện Jpa Lifecycle Callback: Tự động điền thời gian bắt đầu và thiết lập trạng thái ACTIVE
   * trước khi thêm vào DB.
   */
  @PrePersist
  void makeItemActive() {
    this.startTime = Instant.now().toEpochMilli();
    this.itemStatus = "ACTIVE";
  }

  public ItemStatus() {}
  ;

  public ItemStatus(
      Item item,
      Double currentPrice,
      String username,
      Long endTime,
      Double startingPrice,
      Double buyItNowPrice,
      Double bidIncrement,
      Long maxEndTime) {
    this.item = item;
    this.currentPrice = currentPrice;
    this.highestBidUser = username;
    this.endTime = endTime;
    this.startingPrice = startingPrice;
    this.buyItNowPrice = buyItNowPrice;
    this.bidIncrement = bidIncrement;
    this.maxEndTime = maxEndTime;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public Double getCurrentPrice() {
    return currentPrice;
  }

  public void setCurrentPrice(Double currentPrice) {
    this.currentPrice = currentPrice;
  }

  public String getHighestBidUser() {
    return highestBidUser;
  }

  public void setHighestBidUser(String highestBidUser) {
    this.highestBidUser = highestBidUser;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public Double getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(Double startingPrice) {
    this.startingPrice = startingPrice;
  }

  public Double getBuyItNowPrice() {
    return buyItNowPrice;
  }

  public void setBuyItNowPrice(Double buyItNowPrice) {
    this.buyItNowPrice = buyItNowPrice;
  }

  public Double getBidIncrement() {
    return bidIncrement;
  }

  public void setBidIncrement(Double bidIncrement) {
    this.bidIncrement = bidIncrement;
  }

  public String getItemStatus() {
    return itemStatus;
  }

  public void setItemStatus(String itemStatus) {
    this.itemStatus = itemStatus;
  }

  public Long getId() {
    return id;
  }

  public Long getStartTime() {
    return startTime;
  }

  public Long getMaxEndTime() {
    return maxEndTime;
  }

  /**
   * Cập nhật người giữ giá cao nhất mới và tăng giá cược lên bước tiếp theo.
   *
   * @param username Tên đăng nhập người giữ giá cao nhất mới
   */
  public void setNextBidStep(String username) {
    this.highestBidUser = username;
    this.currentPrice = getNextBidStep();
  }

  /**
   * Tính toán số tiền tối thiểu cần để đặt cược ở lượt tiếp theo.
   *
   * @return Số tiền của bước giá tiếp theo
   */
  public Double getNextBidStep() {
    return this.getBidIncrement() + this.getCurrentPrice();
  }
}
