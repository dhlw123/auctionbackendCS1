package com.auction.items.dto;

import com.auction.common.BaseResponse;
import com.auction.items.Item;

public class BaseItemResponse extends BaseResponse {
    private Item item;

    public BaseItemResponse(boolean status, String message, Item item) {
        super(status, message);
        this.item = item;
    }

    // need getters here so that SpringBoot can access them
    public Item getItem() {
        return item;
    }
}