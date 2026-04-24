package com.auction.items.dto;

import com.auction.common.BaseResponse;
import com.auction.items.Item;

import java.util.List;

public class GetItemsResponse extends BaseResponse {
    List<Item> items;

    public GetItemsResponse(Boolean status, String message, List<Item> items) {
        super(status, message);
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }
}
