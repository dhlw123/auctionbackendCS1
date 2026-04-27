package com.auction.items.dto;

import com.auction.common.BaseResponse;
import com.auction.items.Item;

public class PublishItemResponse extends BaseResponse {
    Item item;

    public PublishItemResponse(boolean status, String message, Item item) {
        super(status, message);
        this.item = item;
    }
}