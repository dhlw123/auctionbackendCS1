package com.auction.common;

/**
 * Lớp phản hồi API dạng đối tượng (Generic Wrapper Response), kế thừa từ BaseResponse. Dùng để bọc
 * dữ liệu trả về cho Client cùng với trạng thái và thông điệp.
 *
 * @param <T> Kiểu dữ liệu của thực thể (Entity/DTO) đi kèm trong phản hồi
 */
public class BaseObjectResponse<T> extends BaseResponse {
  // Dữ liệu thực thể được trả về cho client
  private T entity;

  public BaseObjectResponse(boolean status, String message, T entity) {
    super(status, message);
    this.entity = entity;
  }

  public T getEntity() {
    return entity;
  }
}
