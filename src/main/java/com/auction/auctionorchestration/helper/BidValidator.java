package com.auction.auctionorchestration.helper;

import com.auction.common.BaseException;
import com.auction.items.Item;
import com.auction.items.ItemService;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserService;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class BidValidator {

    private final ItemStatusService itemStatusService;
    private final ItemService itemService;
    private final UserService userService;

    public BidValidator(ItemStatusService itemStatusService, ItemService itemService,
            UserService userService) {
        this.itemStatusService = itemStatusService;
        this.itemService = itemService;
        this.userService = userService;
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
        if (auctionEndedOrNot(itemId)) {
            throw new BaseException("Auction has already ended");
        }
    }

    public void validateAboveMinimum(ItemStatus itemStatus, Double value) {
        if (itemStatus.getCurrentPrice() + itemStatus.getBidIncrement() > value) {
            throw new BaseException("Your bid must be higher than the current highest");
        }
    }


    public boolean auctionEndedOrNot(Long itemId) {
        ItemStatus itemStatus = itemStatusService.getItemStatus(itemId);
        // ItemStatus itemStatus = itemStatusRepository.findByItemWithLockByItemId(itemId);

        // Nếu đã ở trạng thái ENDED hoặc CANCELED thì chắc chắn phiên đấu giá đã dừng
        if (itemStatus.getItemStatus().equals("ENDED")
                || itemStatus.getItemStatus().equals("CANCELED")) {
            return true;
        }
        // Nếu đã qua thời gian kết thúc dự kiến nhưng trạng thái chưa cập nhật, đổi sang ENDED và
        // lưu
        // lại DB
        else if (itemStatus.getEndTime() < Instant.now().toEpochMilli()) {
            itemStatus.setItemStatus("ENDED");
            itemStatusService.saveStatus(itemStatus);
            User seller = itemService.getItem(itemId).getUser();
            userService.addBalance(seller.getUsername(), itemStatus.getCurrentPrice());
            return true;
        }
        // Ngược lại, phiên đấu giá vẫn đang mở
        else {
            return false;
        }
    }
}
