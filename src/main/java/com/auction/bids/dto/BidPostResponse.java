package com.auction.bids.dto;

import com.auction.bids.Bid;
import com.auction.common.BaseResponse;

public class BidPostResponse extends BaseResponse {
    Bid bid;

    public BidPostResponse(boolean status, String message, Bid bid) {
        super(status, message);
        this.bid = bid;
    }

    public Bid getBid() {
        return bid;
    }

}
