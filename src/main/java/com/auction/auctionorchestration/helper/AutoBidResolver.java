package com.auction.auctionorchestration.helper;

import com.auction.auctionorchestration.dto.AutoBidRequest;
import com.auction.bids.AutoBid;
import com.auction.bids.BidService;
import com.auction.items.Item;
import com.auction.itemstatus.ItemStatus;
import com.auction.users.User;
import com.auction.users.UserService;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AutoBidResolver {

  private final BidService bidService;
  private final UserService userService;

  public AutoBidResolver(BidService bidService, UserService userService) {
    this.bidService = bidService;
    this.userService = userService;
  }

  public void resolveAgainstManualBid(
      ItemStatus itemStatus, String username, Item item, Double bidAmount, Long itemId) {
    Optional<AutoBid> autoBidOP = bidService.getAutoBidByItemId(itemId);

    if (autoBidOP.isPresent() && !autoBidOP.get().getBidder().getUsername().equals(username)) {
      AutoBid autoBid = autoBidOP.get();

      if (bidAmount + itemStatus.getBidIncrement() > autoBid.getMaxBidLimit()) {
        userService.addBalance(autoBid.getBidder().getUsername(), autoBid.getMaxBidLimit());
        userService.deductBalance(username, bidAmount);
        itemStatus.setHighestBidUser(username);
        itemStatus.setCurrentPrice(bidAmount);
        bidService.deleteAutoBid(autoBid);
      } else {
        double autoCounter =
            Math.min(bidAmount + itemStatus.getBidIncrement(), autoBid.getMaxBidLimit());
        autoBid.setCurrentBidValue(autoCounter);
        bidService.saveAutoBid(autoBid);
        itemStatus.setHighestBidUser(autoBid.getBidder().getUsername());
        itemStatus.setCurrentPrice(autoCounter);
      }
    } else {
      userService.deductBalance(username, bidAmount);

      if (!itemStatus.getHighestBidUser().equals(item.getUser().getUsername())) {
        userService.addBalance(itemStatus.getHighestBidUser(), itemStatus.getCurrentPrice());
      }

      itemStatus.setHighestBidUser(username);
      itemStatus.setCurrentPrice(bidAmount);
    }
  }

  public void resolveBuyNowRefund(ItemStatus itemStatus, Item item, Long itemId) {
    if (!itemStatus.getHighestBidUser().equals(item.getUser().getUsername())) {
      Optional<AutoBid> autoBidOP = bidService.getAutoBidByItemId(itemId);

      if (autoBidOP.isPresent()) {
        AutoBid autoBid = autoBidOP.get();
        boolean isHighestBidderAutoBidding =
            itemStatus.getHighestBidUser().equals(autoBid.getBidder().getUsername());

        if (isHighestBidderAutoBidding) {
          userService.addBalance(autoBid.getBidder().getUsername(), autoBid.getMaxBidLimit());
        } else {
          userService.addBalance(itemStatus.getHighestBidUser(), itemStatus.getCurrentPrice());
        }
      } else {
        userService.addBalance(itemStatus.getHighestBidUser(), itemStatus.getCurrentPrice());
      }
    }
  }

  public void resolveAutoBidCreation(ItemStatus itemStatus, User bidder, AutoBidRequest request) {
    Optional<AutoBid> autoBidOP = bidService.getAutoBidByItemId(request.itemId());
    boolean isSameAutoBidder =
        autoBidOP.isPresent()
            && autoBidOP.get().getBidder().getUsername().equals(bidder.getUsername());

    if (isSameAutoBidder) {
      AutoBid prevAutoBid = autoBidOP.get();
      double oldMax = prevAutoBid.getMaxBidLimit();
      double newMax = request.maxBidLimit();

      if (newMax > oldMax) {
        userService.deductBalance(bidder.getUsername(), newMax - oldMax);
        prevAutoBid.setMaxBidLimit(newMax);
        bidService.saveAutoBid(prevAutoBid);
      } else if (newMax < oldMax) {
        userService.addBalance(bidder.getUsername(), oldMax - newMax);
        prevAutoBid.setMaxBidLimit(newMax);
        bidService.saveAutoBid(prevAutoBid);
      }
    } else if (autoBidOP.isPresent()) {
      AutoBid prevAutoBid = autoBidOP.get();
      User prevUser = prevAutoBid.getBidder();

      if (request.maxBidLimit() > prevAutoBid.getMaxBidLimit()) {
        userService.addBalance(prevUser.getUsername(), prevAutoBid.getMaxBidLimit());
        userService.deductBalance(bidder.getUsername(), request.maxBidLimit());

        AutoBid currentAutoBid =
            new AutoBid(
                request.itemId(), bidder, request.maxBidLimit(), itemStatus.getNextBidStep());
        itemStatus.setNextBidStep(bidder.getUsername());
        bidService.saveAutoBid(currentAutoBid);
      } else {
        prevAutoBid.setCurrentBidValue(request.maxBidLimit());
        itemStatus.setHighestBidUser(prevUser.getUsername());
        itemStatus.setCurrentPrice(prevAutoBid.getCurrentBidValue());
        bidService.saveAutoBid(prevAutoBid);
      }
    } else {
      userService.addBalance(itemStatus.getHighestBidUser(), itemStatus.getCurrentPrice());
      AutoBid currentAutoBid;
      if (itemStatus.getHighestBidUser().equals(bidder.getUsername())) {
        currentAutoBid =
            new AutoBid(
                request.itemId(), bidder, request.maxBidLimit(), itemStatus.getCurrentPrice());
      } else {
        currentAutoBid =
            new AutoBid(
                request.itemId(), bidder, request.maxBidLimit(), itemStatus.getNextBidStep());
      }
      bidService.saveAutoBid(currentAutoBid);
      itemStatus.setCurrentPrice(currentAutoBid.getCurrentBidValue());
      itemStatus.setHighestBidUser(bidder.getUsername());
      userService.deductBalance(bidder.getUsername(), request.maxBidLimit());
    }
  }
}
