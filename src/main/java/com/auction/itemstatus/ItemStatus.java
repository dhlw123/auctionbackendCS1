package com.auction.itemstatus;

import java.time.Instant;

import com.auction.items.Item;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_statuses")
public class ItemStatus {
    // Put this here so that Jpa does not smite me.
    @Id
    private Long id;

    @JsonIgnore
    @OneToOne
    @MapsId
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "current_price")
    private Double currentPrice;

    @Column(name = "username")
    private String highestBidUser;

    // We store as Unix time so backend have less of a headache
    @Column(name = "start_time")
    private Long startTime;

    @PrePersist
    void setStartTime() {
        this.startTime = Instant.now().toEpochMilli();
    }

    @Column(name = "end_time")
    private Long endTime;

    @Column(name = "starting_price")
    private Double startingPrice;

    @Column(name = "buy_it_now_price")
    private Double buyItNowPrice;

    @Column(name = "bid_increment")
    private Double bidIncrement;

    public ItemStatus() {
    };

    public ItemStatus(Item item, Double currentPrice, String username, Long endTime, Double startingPrice,
            Double buyItNowPrice, Double bidIncrement) {
        this.item = item;
        this.currentPrice = currentPrice;
        this.highestBidUser = username;
        this.endTime = endTime;
        this.startingPrice = startingPrice;
        this.buyItNowPrice = buyItNowPrice;
        this.bidIncrement = bidIncrement;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getHighestBidUser() {
        return highestBidUser;
    }

    public void setHighestBidUser(String highestBidUser) {
        this.highestBidUser = highestBidUser;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(Double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public Double getBuyItNowPrice() {
        return buyItNowPrice;
    }

    public void setBuyItNowPrice(Double buyItNowPrice) {
        this.buyItNowPrice = buyItNowPrice;
    }

    public Double getBidIncrement() {
        return bidIncrement;
    }

    public void setBidIncrement(Double bidIncrement) {
        this.bidIncrement = bidIncrement;
    }

}
