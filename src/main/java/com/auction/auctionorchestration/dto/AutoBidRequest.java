package com.auction.auctionorchestration.dto;

import jakarta.validation.constraints.NotNull;

/** Record đại diện cho dữ liệu yêu cầu thiết lập cấu hình tự động đặt giá (Auto-Bid). */
public record AutoBidRequest(
    // Mã ID sản phẩm muốn tự động đặt giá, không được null
    @NotNull Long itemId,

    // Giới hạn số tiền tối đa mà hệ thống có thể tự động nâng cược, không được null
    @NotNull Double maxBidLimit) {}
