package com.auction.users;

import com.auction.common.BaseResponse;
import com.auction.users.exceptions.UserException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.auction.users")
public class UserControllerAdvice {
    @ExceptionHandler(UserException.class)
    public ResponseEntity<BaseResponse> handleNoSellerException(UserException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.response);
    }
}
