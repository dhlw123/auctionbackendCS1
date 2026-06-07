package com.auction.auctionorchestration.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Record đại diện cho dữ liệu yêu cầu thực hiện đặt cược (bid) thủ công cho sản phẩm. */
public record BidPostRequest(
    // Mã ID sản phẩm muốn đặt giá cược, không được null
    @NotNull Long itemId,

    // Số tiền đặt cược, phải là số dương và không được null
    @Positive @NotNull Double bidAmount) {}
