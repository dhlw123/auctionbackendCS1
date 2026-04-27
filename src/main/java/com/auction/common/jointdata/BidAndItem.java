package com.auction.common.jointdata;

import com.auction.bids.Bid;
import com.auction.items.Item;

public class BidAndItem {
    private Bid bid;
    private Item item;

    public BidAndItem(Bid bid, Item item) {
        this.bid = bid;
        this.item = item;
    }

    public Bid getBid() {
        return bid;
    }

    public void setBid(Bid bid) {
        this.bid = bid;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

}
