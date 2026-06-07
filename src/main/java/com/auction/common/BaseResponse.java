package com.auction.common;

/** Lớp phản hồi API cơ sở, định nghĩa cấu trúc dữ liệu chuẩn trả về cho mọi yêu cầu API. */
public class BaseResponse {
  // Trạng thái của yêu cầu (true nếu thành công, false nếu thất bại)
  private boolean status;

  // Thông điệp hoặc mô tả chi tiết đi kèm phản hồi
  private String message;

  // Constructor mặc định cần thiết cho thư viện Jackson thực hiện quá trình Deserialize/Serialize
  // JSON
  public BaseResponse() {}

  public BaseResponse(boolean status, String message) {
    this.status = status;
    this.message = message;
  }

  public boolean getStatus() {
    return status;
  }

  public void setStatus(boolean status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
