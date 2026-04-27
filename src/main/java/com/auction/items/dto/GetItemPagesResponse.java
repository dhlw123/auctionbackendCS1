package com.auction.items.dto;

import org.springframework.data.domain.Page;

import com.auction.common.BaseResponse;
import com.auction.items.Item;

public class GetItemPagesResponse extends BaseResponse {
    Page<Item> pages;

    public GetItemPagesResponse(boolean status, String message, Page<Item> pages) {
        super(status, message);
        this.pages = pages;
    }

    public Page<Item> getPages() {
        return pages;
    }

}
