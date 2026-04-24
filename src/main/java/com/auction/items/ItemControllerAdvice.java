package com.auction.items;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.auction.common.BaseResponse;
import com.auction.items.exceptions.ItemException;

@RestControllerAdvice(basePackages = "com.auction.items") // Error handling safety net
public class ItemControllerAdvice {
    @ExceptionHandler(ItemException.class)
    public ResponseEntity<BaseResponse> handleNoSellerException(ItemException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.response);
    }
}
