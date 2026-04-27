package com.auction.bids;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auction.items.Item;
import com.auction.users.User;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    // JPA Delivery Query Method black magic. Syntax: [Prefix] + [Subject] + [By] +
    // [Property Expression] + [Keyword] + [Logical Operator]
    // Prefix: find, read, get, query, search, stream, exist
    // Subject: entity's name or "Distinct"
    // By: query conditions begin here
    // Property Expression: Must be a variable name in your class (the class here is
    // Bid)
    Bid findByUserAndItem(User user, Item item);

    Page<Bid> findAllByUser(User user, Pageable pageable);

    boolean existsByUserAndItem(User user, Item item);

    // get all bids on an item
    @Query(value = "SELECT b FROM Bid b JOIN FETCH b.item WHERE b.item.itemId = :itemId")
    public Page<Bid> findItemBidHistory(Pageable pageable, @Param("itemId") Long itemId);

    @NativeQuery(value = "SELECT bids.* FROM bids INNER JOIN items ON bids.item_id = items.item_id INNER JOIN item_statuses ON items.item_id = item_statuses.item_id WHERE :username = bids.bidder_username AND item_statuses.end_time < :now")
    public List<Bid> getWinsByUser(@Param("username") String username, @Param("now") Long now);
}
