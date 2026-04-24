package com.auction.common.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NoSpaceValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoSpace {
    //Error message
    String message() default "Spaces are not allowed";
    //Required by Jakarta Validation spec
    Class<?>[] groups() default{};

    Class<? extends Payload>[] payload() default{};

}
