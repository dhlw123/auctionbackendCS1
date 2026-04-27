package com.auction.bids;

import java.time.Instant;

import org.springframework.messaging.simp.user.UserRegistryMessageHandler;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.auction.bids.dto.BidPostRequest;
import com.auction.bids.dto.BidPostResponse;
import com.auction.bids.exceptions.BuyItNowException;
import com.auction.common.BaseException;
import com.auction.common.BaseResponse;
import com.auction.items.Item;
import com.auction.items.ItemRepository;
import com.auction.items.ItemService;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserRepository;
import com.auction.users.UserService;

import jakarta.transaction.Transactional;

@Service
public class BidService {
    // Don't inject repository, inject service instead. Refactor tomorrow.

    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemStatusService itemStatusService;

    public BidService(BidRepository bidRepository, UserRepository userRepository, ItemRepository itemRepository,
            ItemStatusService itemStatusService) {
        this.bidRepository = bidRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.itemStatusService = itemStatusService;
    }

    @Transactional
    public BidPostResponse createBid(BidPostRequest request, String username) {
        Bid bid;
        Item itemRef = itemRepository.getReferenceById(request.itemId());
        User user = userRepository.getReferenceById(username);
        ItemStatus itemStatus = itemStatusService.getItemStatus(request.itemId());

        // big amount must be higher than starting price and bid time must be lower than
        // endtime and bid amount must be higher than current balance
        if (request.bidAmount() < itemStatus.getStartingPrice()
                || Instant.now().toEpochMilli() < itemStatus.getEndTime() || request.bidAmount() > user.getBalance()) {
            throw new BaseException(
                    "Failed to bid");
        }

        // if bid exist then get bid from DB and then edit bid and save it again to db
        if (bidRepository.existsByUserAndItem(user, itemRef)) {
            bid = bidRepository.findByUserAndItem(user, itemRef);
            bid.setBidAmount(request.bidAmount());
            bidRepository.save(bid);
        } else { // Else make new bid
            bid = new Bid(itemRef, user, request.bidAmount());
            bidRepository.save(bid);
        }
        // If user bid amount if higher than the current highest + increment, they would
        // be the highest bidder
        if (request.bidAmount() > itemStatus.getCurrentPrice() + itemStatus.getBidIncrement()) {
            itemStatusService.updateStatus(itemRef, request.bidAmount(), username);
        }
        return new BidPostResponse(true, "Successfully created bid for an item", bid);
    }

    @Transactional
    public BaseResponse buyItemNow(Long itemId, String username) {
        ItemStatus itemStatus = itemStatusService.getItemStatus(itemId);
        User user = userRepository.findById(username).orElseThrow(() -> new UsernameNotFoundException(username));
        // Buy now if balance >= buyitnow, buyitnow > currentprice, buyitnow != 0
        // (Seller does not set a buy now price)
        if (user.getBalance() >= itemStatus.getBuyItNowPrice()
                && itemStatus.getBuyItNowPrice() > itemStatus.getCurrentPrice() && itemStatus.getBuyItNowPrice() != 0) {
            itemStatus.setHighestBidUser(username);
            itemStatus.setCurrentPrice(itemStatus.getBuyItNowPrice());
            itemStatus.setEndTime(Instant.now().toEpochMilli());
            itemStatusService.saveStatus(itemStatus);
        } else {

            throw new BuyItNowException("You don't have enough money in your balance to buy the item");
        }
        return new BaseResponse(true, "Successfully bought item");
    }

}
