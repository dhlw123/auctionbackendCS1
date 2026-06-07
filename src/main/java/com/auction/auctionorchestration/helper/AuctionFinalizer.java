package com.auction.auctionorchestration.helper;

import com.auction.bids.AutoBid;
import com.auction.bids.BidService;
import com.auction.items.ItemService;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusRepository;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuctionFinalizer {

  private static final Logger log = LoggerFactory.getLogger(AuctionFinalizer.class);

  private final ItemStatusRepository itemStatusRepository;
  private final ItemStatusService itemStatusService;
  private final ItemService itemService;
  private final UserService userService;
  private final BidService bidService;

  public AuctionFinalizer(
      ItemStatusRepository itemStatusRepository,
      ItemStatusService itemStatusService,
      ItemService itemService,
      UserService userService,
      BidService bidService) {
    this.itemStatusRepository = itemStatusRepository;
    this.itemStatusService = itemStatusService;
    this.itemService = itemService;
    this.userService = userService;
    this.bidService = bidService;
  }

  @Scheduled(fixedDelayString = "${auction.finalizer.delay:30000}")
  @Transactional
  public void finalizeExpiredAuctions() {
    Long now = Instant.now().toEpochMilli();
    List<ItemStatus> expired = itemStatusRepository.findByItemStatusAndEndTimeBefore("ACTIVE", now);

    for (ItemStatus status : expired) {
      Long itemId = status.getItem().getItemId();
      ItemStatus locked = itemStatusService.getItemStatus(itemId);

      if ("ACTIVE".equals(locked.getItemStatus()) && locked.getEndTime() < now) {
        locked.setItemStatus("ENDED");
        itemStatusService.saveStatus(locked);

        User seller = itemService.getItem(itemId).getUser();
        userService.addBalance(seller.getUsername(), locked.getCurrentPrice());

        // checks if user is auto bidding or not
        Optional<AutoBid> autobidOP = bidService.getAutoBidByItemId(itemId);
        if (autobidOP.isPresent()
            && autobidOP.get().getBidder().getUsername().equals(locked.getHighestBidUser())) {
          User autoBidder = autobidOP.get().getBidder();
          userService.addBalance(
              autoBidder.getUsername(),
              autobidOP.get().getMaxBidLimit() - locked.getCurrentPrice());
        }

        log.info(
            "Finalized auction for item {}: paid {} to {}",
            itemId,
            locked.getCurrentPrice(),
            seller.getUsername());
      }
    }
  }
}
