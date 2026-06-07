package com.auction.common.jointdata;

import com.auction.bids.Bid;
import com.auction.items.Item;

/**
 * Lớp kết hợp thông tin (Wrapper DTO) chứa cả thông tin cược (Bid) và sản phẩm đấu giá (Item). Được
 * sử dụng để đóng gói và gửi đồng thời dữ liệu của hai đối tượng này về cho Client.
 */
public class BidAndItem {
  // Chi tiết thông tin lượt đặt cược
  private Bid bid;

  // Chi tiết thông tin sản phẩm đấu giá liên quan
  private Item item;

  public BidAndItem(Bid bid, Item item) {
    this.bid = bid;
    this.item = item;
  }

  public Bid getBid() {
    return bid;
  }

  public void setBid(Bid bid) {
    this.bid = bid;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }
}
