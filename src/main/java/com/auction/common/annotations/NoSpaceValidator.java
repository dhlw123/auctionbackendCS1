package com.auction.common.annotations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoSpaceValidator implements ConstraintValidator<NoSpace, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        //Null value considered seperately
        if (value == null) {
            return true;
        }

        //Returns false if string contains whitespace
        return !value.contains(" ");
    }
}
