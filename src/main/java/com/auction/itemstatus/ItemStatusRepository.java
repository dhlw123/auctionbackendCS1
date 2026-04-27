package com.auction.itemstatus;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auction.items.Item;

public interface ItemStatusRepository extends JpaRepository<ItemStatus, Item> {
    ItemStatus findByItem(Item item);
}
