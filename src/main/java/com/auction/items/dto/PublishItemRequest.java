package com.auction.items.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Record đại diện cho dữ liệu yêu cầu đăng bán đấu giá sản phẩm mới. */
public record PublishItemRequest(
    // Tiêu đề của sản phẩm, không được trống
    @NotBlank String title,

    // Mô tả chi tiết về sản phẩm, không được null
    @NotNull String description,

    // Thời gian kết thúc phiên đấu giá dưới dạng Epoch Milliseconds, không được null
    @NotNull Long endTime,

    // Giá khởi điểm của sản phẩm, phải là số dương
    @Positive @NotNull Double startingPrice,

    // Giá mua ngay (Buy It Now) giúp kết thúc đấu giá sớm, phải là số dương
    @Positive @NotNull Double buyItNowPrice,

    // Bước giá tối thiểu cho mỗi lần đặt cược tiếp theo, phải là số dương
    @Positive @NotNull Double bidIncrement) {}
