package com.auction.items;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auction.users.User;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    public Page<Item> findByUser(Pageable pageable, User user);

    @NativeQuery(value = "SELECT * FROM items INNER JOIN item_statuses ON items.item_id = item_statuses.item_id WHERE :now < item_statuses.end_time", countQuery = "SELECT count(*) FROM items INNER JOIN item_statuses ON items.item_id = item_statuses.item_id WHERE :now < item_statuses.end_time")
    public Page<Item> findActiveItemPages(@Param("now") long now, Pageable pageable);

    @NativeQuery(value = "SELECT * FROM items RIGHT JOIN bids ON items.item_id = bids.item_id", countQuery = "SELECT count(*) FROM items RIGHT JOIN bids ON items.item_id = bids.item_id")
    public Page<Item> findBidsOnItem(Pageable pageable);
}
