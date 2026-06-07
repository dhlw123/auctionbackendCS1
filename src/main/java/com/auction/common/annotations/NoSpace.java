package com.auction.common.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/** Annotation xác thực tùy chỉnh: Ràng buộc giá trị chuỗi không được chứa khoảng trắng. */
@Documented
@Constraint(validatedBy = NoSpaceValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoSpace {
  // Thông điệp lỗi mặc định khi xác thực thất bại
  String message() default "Spaces are not allowed";

  // Thuộc tính bắt buộc theo đặc tả của Jakarta Bean Validation để phân nhóm kiểm tra
  Class<?>[] groups() default {};

  // Thuộc tính bắt buộc chứa thông tin bổ sung đi kèm lỗi (payload)
  Class<? extends Payload>[] payload() default {};
}
