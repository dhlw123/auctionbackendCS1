package com.auction.auctionorchestration.helper;

import org.springframework.stereotype.Component;

import com.auction.common.BaseException;
import com.auction.items.Item;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;

@Component
public class BidValidator {

    private final ItemStatusService itemStatusService;

    public BidValidator(ItemStatusService itemStatusService) {
        this.itemStatusService = itemStatusService;
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
        if (itemStatusService.auctionEndedOrNot(itemId)) {
            throw new BaseException("Auction has already ended");
        }
    }

    public void validateAboveMinimum(ItemStatus itemStatus, Double value) {
        if (itemStatus.getCurrentPrice() + itemStatus.getBidIncrement() > value) {
            throw new BaseException("Your bid must be higher than the current highest");
        }
    }
}
