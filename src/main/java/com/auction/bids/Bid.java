package com.auction.bids;

import java.time.Instant;

import com.auction.items.Item;
import com.auction.users.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "bids")
public class Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_id")
    private Long bidId;

    @JsonIgnore
    @ManyToOne // Many bids, Item
    @JoinColumn(name = "item_id")
    private Item item;

    @JsonIgnore
    @ManyToOne // One Bidder many Bid
    @JoinColumn(name = "bidder_username")
    private User user;

    @Column(name = "bid_amount")
    private Double bidAmount;

    @Column(name = "bid_time")
    private Long time;

    @PrePersist
    protected void addTime() {
        time = Instant.now().toEpochMilli();
    }

    public Bid() {
    };

    public Bid(Item item, User bidder_username, Double bidAmount) {
        this.item = item;
        this.user = bidder_username;
        this.bidAmount = bidAmount;
    }

    public Long getBidId() {
        return bidId;
    }

    public void setBidId(Long bidId) {
        this.bidId = bidId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(Double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

}
