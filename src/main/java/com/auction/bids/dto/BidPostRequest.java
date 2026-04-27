package com.auction.bids.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BidPostRequest(
        @NotNull Long itemId, @Positive @NotNull Double bidAmount) {
}