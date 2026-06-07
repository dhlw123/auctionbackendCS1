package com.auction.auctionorchestration;

import com.auction.auctionorchestration.dto.AutoBidRequest;
import com.auction.auctionorchestration.dto.BidPostRequest;
import com.auction.auctionorchestration.helper.AutoBidResolver;
import com.auction.auctionorchestration.helper.BidValidator;
import com.auction.bids.AutoBid;
import com.auction.bids.Bid;
import com.auction.bids.BidService;
import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;
import com.auction.common.BaseResponse;
import com.auction.common.jointdata.BidAndItem;
import com.auction.items.Item;
import com.auction.items.ItemPricesSink;
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

  private final BidValidator bidValidator;
  private final AutoBidResolver autoBidResolver;
  private final ItemService itemService;
  private final UserService userService;
  private final ItemStatusService itemStatusService;
  private final BidService bidService;
  private final ItemPricesSink itemPricesSink;

  @Value("${extra_time}")
  private Long extraTime;

  public AuctionService(
      BidValidator bidValidator,
      AutoBidResolver autoBidResolver,
      ItemService itemService,
      UserService userService,
      ItemStatusService itemStatusService,
      BidService bidService,
      ItemPricesSink itemPricesSink) {
    this.bidValidator = bidValidator;
    this.autoBidResolver = autoBidResolver;
    this.itemService = itemService;
    this.userService = userService;
    this.itemStatusService = itemStatusService;
    this.bidService = bidService;
    this.itemPricesSink = itemPricesSink;
  }

  @Transactional
  public BaseObjectResponse<Bid> createBid(BidPostRequest request, String username) {
    Item item = itemService.getItemRef(request.itemId());
    User user = userService.getUserRef(username);
    ItemStatus itemStatus = itemStatusService.getItemStatus(request.itemId());

    bidValidator.validate(item, user, itemStatus, request.bidAmount());

    Bid bid = upsertBid(user, item, request.bidAmount());
    autoBidResolver.resolveAgainstManualBid(
        itemStatus, username, item, request.bidAmount(), request.itemId());

    finalizeBid(itemStatus, request.itemId());
    return new BaseObjectResponse<>(true, "Successfully created bid for an item", bid);
  }

  @Transactional(readOnly = true)
  public BaseObjectResponse<Page<Bid>> getMyCurrentBids(String username, int page, int size) {
    PageRequest pageable = PageRequest.of(page, size);
    User userRef = userService.getUserRef(username);
    Page<Bid> bids = bidService.getAllUserBid(userRef, pageable);
    return new BaseObjectResponse<>(true, "succesfully got my bids", bids);
  }

  @Transactional(readOnly = true)
  public BaseObjectResponse<List<BidAndItem>> getMyWinnings(String username) {
    List<Bid> bids = bidService.getUserWins(username);
    ArrayList<BidAndItem> items = new ArrayList<>();
    for (Bid bid : bids) {
      items.add(new BidAndItem(bid, bid.getItem()));
    }
    return new BaseObjectResponse<>(true, "successfully returned winnings", items);
  }

  @Transactional
  public BaseResponse buyItemNow(Long itemId, String username) {
    ItemStatus itemStatus = itemStatusService.getItemStatus(itemId);
    User user = userService.getUserByUsername(username);
    Item item = itemService.getItem(itemId);

    bidValidator.validate(item, user, itemStatus, itemStatus.getBuyItNowPrice());

    bidService.saveBid(new Bid(item, user, itemStatus.getBuyItNowPrice()));
    userService.deductBalance(username, itemStatus.getBuyItNowPrice());

    autoBidResolver.resolveBuyNowRefund(itemStatus, item, itemId);

    itemStatus.setHighestBidUser(username);
    itemStatus.setCurrentPrice(itemStatus.getBuyItNowPrice());
    itemStatus.setEndTime(Instant.now().toEpochMilli());

    itemStatusService.saveStatus(itemStatus);
    itemPricesSink.publishPrice(itemId, itemStatus.getCurrentPrice());
    return new BaseResponse(true, "Successfully bought item");
  }

  @Transactional
  public BaseResponse createAutoBid(AutoBidRequest request, String bidderName) {
    User bidder = userService.getUserByUsername(bidderName);
    Item item = itemService.getItem(request.itemId());
    ItemStatus itemStatus = itemStatusService.getItemStatus(request.itemId());

    bidValidator.validate(item, bidder, itemStatus, request.maxBidLimit());

    if (itemStatus.getCurrentPrice() == 0) {
      itemStatus.setHighestBidUser(bidderName);
      itemStatus.setCurrentPrice(itemStatus.getStartingPrice());

      userService.deductBalance(bidderName, request.maxBidLimit());

      AutoBid autoBid =
          new AutoBid(
              request.itemId(), bidder, request.maxBidLimit(), itemStatus.getStartingPrice());
      bidService.saveAutoBid(autoBid);
    } else {
      autoBidResolver.resolveAutoBidCreation(itemStatus, bidder, request);
    }

    finalizeBid(itemStatus, request.itemId());
    return new BaseResponse(true, "succesfully make auto bid");
  }

  private Bid upsertBid(User user, Item item, Double bidAmount) {
    if (bidService.existUserAndItem(user, item)) {
      Bid existingBid = bidService.getBidByUserAndItem(user, item);
      existingBid.setBidAmount(bidAmount);
      bidService.saveBid(existingBid);
      return existingBid;
    }
    Bid newBid = new Bid(item, user, bidAmount);
    bidService.saveBid(newBid);
    return newBid;
  }

  private void finalizeBid(ItemStatus itemStatus, Long itemId) {
    applyAntiBidExtension(itemStatus);
    itemStatusService.saveStatus(itemStatus);
    itemPricesSink.publishPrice(itemId, itemStatus.getCurrentPrice());
  }

  private void applyAntiBidExtension(ItemStatus itemStatus) {
    Long remainingTime = itemStatus.getEndTime() - Instant.now().toEpochMilli();
    if (remainingTime < extraTime && itemStatus.getEndTime() < itemStatus.getMaxEndTime()) {
      itemStatus.setEndTime(Instant.now().toEpochMilli() + extraTime);
    }
  }

  /**
   * Thực hiện nghiệp vụ hủy bỏ một phiên đấu giá sản phẩm đang hoạt động. Hoàn lại số tiền cược cho
   * người đặt giá cao nhất hiện tại (nếu có).
   *
   * @param itemId Mã ID sản phẩm cần hủy
   * @param username Tên đăng nhập của người dùng yêu cầu hủy
   * @return BaseResponse phản hồi trạng thái kết quả
   */
  @Transactional
  public BaseResponse cancelItem(Long itemId, String username) {
    Item item = itemService.getItem(itemId);

    // Xác thực quyền sở hữu: Chỉ người đăng bán sản phẩm mới được phép hủy
    if (!item.getUser().getUsername().equals(username)) {
      throw new BaseException("You are not the owner of this item");
    }

    ItemStatus status = itemStatusService.getItemStatus(itemId);
    // Chỉ cho phép hủy các sản phẩm có trạng thái đấu giá là ACTIVE và chưa thực sự kết thúc
    if (!"ACTIVE".equals(status.getItemStatus()) || bidValidator.auctionEndedOrNot(itemId)) {
      throw new BaseException("Only ACTIVE items can be canceled.");
    }

    // Cập nhật trạng thái thành CANCELED và kết thúc thời gian đấu giá ngay lập tức
    status.setItemStatus("CANCELED");
    status.setEndTime(Instant.now().toEpochMilli());
    itemStatusService.saveStatus(status);

    // Nghiệp vụ hoàn tiền cược cho người trả giá cao nhất hiện tại (nếu họ không phải người bán
    // và
    // đã trả giá > 0)
    String highestBidUser = status.getHighestBidUser();
    if (!highestBidUser.equals(item.getUser().getUsername()) && status.getCurrentPrice() > 0) {
      userService.addBalance(highestBidUser, status.getCurrentPrice());
    }

    // Hoàn trả toàn bộ maxBidLimit cho người dùng đang có cấu hình tự động đấu giá (AutoBid).
    Optional<AutoBid> autoBidOpt = bidService.getAutoBidByItemId(itemId);
    if (autoBidOpt.isPresent()) {
      AutoBid autoBid = autoBidOpt.get();
      String autoBidderName = autoBid.getBidder().getUsername();

      if (!autoBidderName.equals(item.getUser().getUsername())) {
        if (autoBidderName.equals(highestBidUser)) {
          double remainder = autoBid.getMaxBidLimit() - status.getCurrentPrice();
          if (remainder > 0) {
            userService.addBalance(autoBidderName, remainder);
          }
        } else {
          userService.addBalance(autoBidderName, autoBid.getMaxBidLimit());
        }
        bidService.deleteAutoBid(autoBid);
      }
    }

    return new BaseResponse(true, "Item successfully canceled.");
  }
}
