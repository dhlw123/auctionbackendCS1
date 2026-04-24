package com.auction.items.dto;

public record PublishItemRequest(
        String sellerUsername,
        String title) {
}