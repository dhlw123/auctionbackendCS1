package com.auction.common.annotations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Trình xác thực (Validator) thực thi logic kiểm tra cho annotation @NoSpace. */
public class NoSpaceValidator implements ConstraintValidator<NoSpace, String> {
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // Giá trị null được coi là hợp lệ (việc kiểm tra null sẽ do @NotNull đảm nhiệm riêng biệt)
    if (value == null) {
      return true;
    }

    // Trả về false nếu chuỗi ký tự chứa bất kỳ ký tự khoảng trắng nào
    return !value.contains(" ");
  }
}
