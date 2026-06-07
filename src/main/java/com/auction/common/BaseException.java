package com.auction.common;

/**
 * Ngoại lệ cơ sở (Custom RuntimeException) được sử dụng để ném ra các lỗi nghiệp vụ trong ứng dụng.
 * Tự động đóng gói thông điệp lỗi thành một đối tượng BaseResponse thất bại.
 */
public class BaseException extends RuntimeException {
  // Đối tượng chứa thông tin phản hồi lỗi trả về cho client
  BaseResponse response;

  public BaseException(String message) {
    super(message);
    this.response = new BaseResponse(false, message);
  }

  public BaseResponse getResponse() {
    return response;
  }
}
