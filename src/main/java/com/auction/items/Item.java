package com.auction.items;

import com.auction.users.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Thực thể Item đại diện cho một mặt hàng được đăng bán đấu giá trong hệ thống. */
@Entity
@Table(name = "items")
public class Item {

  // Mã sản phẩm (Khóa chính), tự động tăng
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_id")
  private Long itemId;

  // Người bán sản phẩm. Sử dụng @JsonIgnore để tránh vòng lặp tuần tự hóa hoặc trả thông tin nhạy
  // cảm của User.
  @JsonIgnore
  @ManyToOne
  @JoinColumn(name = "seller_username")
  private User user;

  // Tiêu đề của sản phẩm
  @Column(name = "title", nullable = false)
  private String title;

  // Mô tả chi tiết về sản phẩm
  @Column(name = "description")
  private String description;

  // Constructor mặc định cho JPA
  public Item() {}

  // Constructor đầy đủ tham số (không chứa itemId vì tự động tăng)
  public Item(User user, String title, String description) {
    this.user = user;
    this.title = title;
    this.description = description;
  }

  public Long getItemId() {
    return itemId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Thuộc tính ảo "seller_username" khi tuần tự hóa JSON trả về cho Client. Phương thức này trích
   * xuất tên người bán từ thực thể User liên kết.
   */
  @JsonProperty("seller_username")
  public String getSellerUsername() {
    return user.getUsername();
  }
}
