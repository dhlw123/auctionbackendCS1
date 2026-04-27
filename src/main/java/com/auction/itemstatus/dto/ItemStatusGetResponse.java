package com.auction.itemstatus.dto;

import com.auction.common.BaseResponse;
import com.auction.itemstatus.ItemStatus;

public class ItemStatusGetResponse extends BaseResponse {
    private ItemStatus itemStatus;

    public ItemStatusGetResponse(boolean status, String message, ItemStatus itemStatus) {
        super(status, message);
        this.itemStatus = itemStatus;
    }

    public ItemStatus getItemStatus() {
        return itemStatus;
    }

}
