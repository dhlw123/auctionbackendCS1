package com.auction.auctionorchestration.helper;

import com.auction.items.ItemService;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusRepository;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserService;
import java.time.Instant;
import java.util.List;
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

  public AuctionFinalizer(
      ItemStatusRepository itemStatusRepository,
      ItemStatusService itemStatusService,
      ItemService itemService,
      UserService userService) {
    this.itemStatusRepository = itemStatusRepository;
    this.itemStatusService = itemStatusService;
    this.itemService = itemService;
    this.userService = userService;
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

        log.info(
            "Finalized auction for item {}: paid {} to {}",
            itemId,
            locked.getCurrentPrice(),
            seller.getUsername());
      }
    }
  }
}
