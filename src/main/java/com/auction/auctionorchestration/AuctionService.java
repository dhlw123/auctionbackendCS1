package com.auction.auctionorchestration;

import com.auction.auctionorchestration.dto.AutoBidRequest;
import com.auction.auctionorchestration.dto.BidPostRequest;
import com.auction.bids.AutoBid;
import com.auction.bids.Bid;
import com.auction.bids.BidService;
import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;
import com.auction.common.BaseResponse;
import com.auction.common.ItemPricesSink;
import com.auction.common.jointdata.BidAndItem;
import com.auction.items.Item;
import com.auction.items.ItemService;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionService {

    private final ItemService itemService;
    private final UserService userService;
    private final ItemStatusService itemStatusService;
    private final BidService bidService;
    private final ItemPricesSink itemPricesSink;

    @Value("${extra_time}")
    private Long extraTime;

    public AuctionService(
            ItemService itemService,
            UserService userService,
            ItemStatusService itemStatusService,
            BidService bidService,
            ItemPricesSink itemPricesSink
    ) {
        this.itemService = itemService;
        this.userService = userService;
        this.itemStatusService = itemStatusService;
        this.bidService = bidService;
        this.itemPricesSink = itemPricesSink;
    }

    @Transactional
    public BaseObjectResponse<Bid> createBid(
            BidPostRequest request,
            String username
    ) {
        Bid bid;
        Item item = itemService.getItemRef(request.itemId());
        User user = userService.getUserRef(username);

        ItemStatus itemStatus = itemStatusService.getItemStatus(
                request.itemId()
        );

        validateBasicBidRequirement(
                item,
                user,
                itemStatus,
                request.bidAmount()
        );

        // check if user already has a bid on the item or not.
        if (bidService.existUserAndItem(user, item)) {
            bid = bidService.getBidByUserAndItem(user, item);
            bid.setBidAmount(request.bidAmount());
            bidService.saveBid(bid);
        } else {
            bid = new Bid(item, user, request.bidAmount());
            bidService.saveBid(bid);
        }

        Optional<AutoBid> autoBidOP = bidService.getAutoBidByItemId(
                request.itemId()
        );

        if (
                autoBidOP.isPresent() &&
                        !autoBidOP.get().getBidder().getUsername().equals(username)
        ) {
            AutoBid autoBid = autoBidOP.get();

            if (request.bidAmount() + itemStatus.getBidIncrement() > autoBid.getMaxBidLimit()) {
                User autoUser = autoBid.getBidder();
                userService.addBalance(autoUser.getUsername(), autoBid.getMaxBidLimit());
                userService.deductBalance(username, request.bidAmount());

                updateItemStatusHighestBidder(
                        itemStatus,
                        username,
                        request.bidAmount()
                );
            } else {
                double autoCounter = Math.min(
                        request.bidAmount() + itemStatus.getBidIncrement(),
                        autoBid.getMaxBidLimit()
                );

                autoBid.setCurrentBidValue(autoCounter);
                bidService.saveAutoBid(autoBid);

                updateItemStatusHighestBidder(
                        itemStatus,
                        autoBid.getBidder().getUsername(),
                        autoCounter
                );
            }
        } else {
            userService.deductBalance(username, request.bidAmount());

            if (
                    !itemStatus
                            .getHighestBidUser()
                            .equals(item.getUser().getUsername())
            ) {
                userService.addBalance(itemStatus.getHighestBidUser(), itemStatus.getCurrentPrice());
            }

            updateItemStatusHighestBidder(
                    itemStatus,
                    username,
                    request.bidAmount()
            );
        }

        applyAntiBidExtension(itemStatus);

        itemStatusService.saveStatus(itemStatus);
        itemPricesSink.publishPrice(
                request.itemId(),
                itemStatus.getCurrentPrice()
        );
        return new BaseObjectResponse<>(
                true,
                "Successfully created bid for an item",
                bid
        );
    }

    @Transactional(readOnly = true)
    public BaseObjectResponse<Page<Bid>> getMyCurrentBids(
            String username,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(page, size);
        User userRef = userService.getUserRef(username);

        Page<Bid> bids = bidService.getAllUserBid(userRef, pageable);

        return new BaseObjectResponse<Page<Bid>>(
                true,
                "succesfully got my bids",
                bids
        );
    }

    @Transactional(readOnly = true)
    public BaseObjectResponse<List<BidAndItem>> getMyWinnings(String username) {
        List<Bid> bids = bidService.getUserWins(username);
        ArrayList<BidAndItem> items = new ArrayList<BidAndItem>();
        for (Bid bid : bids) {
            items.add(new BidAndItem(bid, bid.getItem()));
        }
        return new BaseObjectResponse<List<BidAndItem>>(
                true,
                "successfully returned winnings",
                items
        );
    }

    @Transactional
    public BaseResponse buyItemNow(Long itemId, String username) {
        ItemStatus itemStatus = itemStatusService.getItemStatus(itemId);
        User user = userService.getUserByUsername(username);
        Item item = itemService.getItem(itemId);
        validateBasicBidRequirement(
                item,
                user,
                itemStatus,
                itemStatus.getBuyItNowPrice()
        );

        Bid bid = new Bid(item, user, itemStatus.getBuyItNowPrice());
        bidService.saveBid(bid);
        userService.deductBalance(username, itemStatus.getBuyItNowPrice());

        if (
                !itemStatus.getHighestBidUser().equals(item.getUser().getUsername())
        ) {
            userService.addBalance(itemStatus.getHighestBidUser(), itemStatus.getCurrentPrice());
        }

        updateItemStatusHighestBidder(
                itemStatus,
                username,
                itemStatus.getBuyItNowPrice()
        );
        itemStatus.setEndTime(Instant.now().toEpochMilli());
        itemStatusService.saveStatus(itemStatus);

        itemPricesSink.publishPrice(itemId, itemStatus.getBuyItNowPrice());

        return new BaseResponse(true, "Successfully bought item");
    }

    @Transactional
    public BaseResponse createAutoBid(
            AutoBidRequest request,
            String bidderName
    ) {
        User bidder = userService.getUserByUsername(bidderName);
        Optional<AutoBid> autoBidOP = bidService.getAutoBidByItemId(
                request.itemId()
        );
        ItemStatus itemStatus = itemStatusService.getItemStatus(
                request.itemId()
        );
        AutoBid currentAutoBid = new AutoBid(
                request.itemId(),
                bidder,
                request.maxBidLimit(),
                null
        );
        Item item = itemService.getItem(request.itemId());
        validateBasicBidRequirement(
                item,
                bidder,
                itemStatus,
                request.maxBidLimit()
        );

        boolean isSameAutoBidder =
                autoBidOP.isPresent() &&
                        autoBidOP.get().getBidder().getUsername().equals(bidderName);

        if (isSameAutoBidder) {
            AutoBid prevAutoBid = autoBidOP.get();
            double oldMax = prevAutoBid.getMaxBidLimit();
            double newMax = request.maxBidLimit();

            if (newMax > oldMax) {
                userService.deductBalance(bidderName, newMax - oldMax);
                prevAutoBid.setMaxBidLimit(newMax);
                bidService.saveAutoBid(prevAutoBid);
            } else if (newMax < oldMax) {
                userService.addBalance(bidderName, oldMax - newMax);
                prevAutoBid.setMaxBidLimit(newMax);
                bidService.saveAutoBid(prevAutoBid);
            }
        } else if (autoBidOP.isPresent()) {
            AutoBid prevAutoBid = autoBidOP.get();
            User prevUser = prevAutoBid.getBidder();

            if (request.maxBidLimit() > prevAutoBid.getMaxBidLimit()) {
                userService.addBalance(prevUser.getUsername(), prevAutoBid.getMaxBidLimit());
                userService.deductBalance(bidderName, request.maxBidLimit());
                currentAutoBid.setCurrentBidValue(itemStatus.getNextBidStep());

                itemStatus.setNextBidStep(bidderName);

                bidService.saveAutoBid(currentAutoBid);
            } else {
                prevAutoBid.setCurrentBidValue(request.maxBidLimit());
                updateItemStatusHighestBidder(
                        itemStatus,
                        prevUser.getUsername(),
                        prevAutoBid.getCurrentBidValue()
                );
                bidService.saveAutoBid(prevAutoBid);
            }
            itemStatusService.saveStatus(itemStatus);
        } else {
            userService.addBalance(itemStatus.getHighestBidUser(), itemStatus.getCurrentPrice());

            currentAutoBid.setCurrentBidValue(itemStatus.getNextBidStep());
            bidService.saveAutoBid(currentAutoBid);

            itemStatus.setNextBidStep(bidderName);
            itemStatusService.saveStatus(itemStatus);

            userService.deductBalance(bidderName, request.maxBidLimit());
        }
        itemPricesSink.publishPrice(request.itemId(), itemStatus.getCurrentPrice());
        applyAntiBidExtension(itemStatus);
        return new BaseResponse(true, "succesfully make auto bid");
    }

    private void validateBasicBidRequirement(
            Item item,
            User user,
            ItemStatus itemStatus,
            Double value
    ) {
        if (item.getUser().getUsername().equals(user.getUsername())) {
            throw new BaseException("You can't place bid on your own item");
        }
        if (itemStatus.getStartingPrice() > value) {
            throw new BaseException("Your bid must be higher than the starting price");
        }
        validateAuctionNotEnded(item.getItemId());
        validateUserHaveEnoughMoney(user, value);
        validateHigherThanCurrentPrice(itemStatus, value);
    }

    private void validateUserHaveEnoughMoney(User user, Double value) {
        if (user.getBalance() < value) {
            throw new BaseException("You don't have enough money");
        }
    }

    private void validateAuctionNotEnded(Long itemId) {
        if (itemStatusService.auctionEndedOrNot(itemId)) {
            throw new BaseException("Auction has already ended");
        }
    }

    private void updateItemStatusHighestBidder(
            ItemStatus itemStatus,
            String username,
            Double bidAmount
    ) {
        itemStatus.setHighestBidUser(username);
        itemStatus.setCurrentPrice(bidAmount);
    }

    private void validateHigherThanCurrentPrice(
            ItemStatus itemStatus,
            Double value
    ) {
        if (
                itemStatus.getCurrentPrice() + itemStatus.getBidIncrement() > value
        ) {
            throw new BaseException(
                    "Your bid must be higher than the current highest"
            );
        }
    }

    private void applyAntiBidExtension(ItemStatus itemStatus) {
        Long remainingTime =
                itemStatus.getEndTime() - Instant.now().toEpochMilli();
        if (
                remainingTime < extraTime &&
                        itemStatus.getEndTime() < itemStatus.getMaxEndTime()
        ) {
            itemStatus.setEndTime(Instant.now().toEpochMilli() + extraTime);
        }
        itemStatusService.saveStatus(itemStatus);
    }
}
