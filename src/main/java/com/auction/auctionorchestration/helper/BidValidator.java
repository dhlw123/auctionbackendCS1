package com.auction.auctionorchestration.helper;

import com.auction.common.BaseException;
import com.auction.items.Item;
import com.auction.items.ItemService;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserService;
import java.time.Instant;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BidValidator {

    private final ItemStatusService itemStatusService;
    private final ItemService itemService;
    private final UserService userService;
    private final BidValidator self;

    public BidValidator(ItemStatusService itemStatusService, ItemService itemService,
            UserService userService, @Lazy BidValidator self) {
        this.itemStatusService = itemStatusService;
        this.itemService = itemService;
        this.userService = userService;
        this.self = self;
    }

    public void validate(Item item, User user, ItemStatus itemStatus, Double value) {
        if (item.getUser().getUsername().equals(user.getUsername())) {
            throw new BaseException("You can't place bid on your own item");
        }
        if (itemStatus.getStartingPrice() > value) {
            throw new BaseException("Your bid must be higher than the starting price");
        }
        validateAuctionActive(item.getItemId());
        validateSufficientFunds(user, value);
        validateAboveMinimum(itemStatus, value);
    }

    public void validateSufficientFunds(User user, Double value) {
        if (user.getBalance() < value) {
            throw new BaseException("You don't have enough money");
        }
    }

    public void validateAuctionActive(Long itemId) {
        if (self.auctionEndedOrNot(itemId)) {
            throw new BaseException("Auction has already ended");
        }
    }

    public void validateAboveMinimum(ItemStatus itemStatus, Double value) {
        if (itemStatus.getCurrentPrice() + itemStatus.getBidIncrement() > value) {
            throw new BaseException("Your bid must be higher than the current highest");
        }
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean auctionEndedOrNot(Long itemId) {
        ItemStatus itemStatus = itemStatusService.getItemStatus(itemId);

        if (itemStatus.getItemStatus().equals("ENDED")
                || itemStatus.getItemStatus().equals("CANCELED")) {
            return true;
        }
        else if (itemStatus.getEndTime() < Instant.now().toEpochMilli()) {
            itemStatus.setItemStatus("ENDED");
            itemStatusService.saveStatus(itemStatus);
            User seller = itemService.getItem(itemId).getUser();
            userService.addBalance(seller.getUsername(), itemStatus.getCurrentPrice());
            return true;
        }
        else {
            return false;
        }
    }
}
