package com.auction.auctionorchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuctionServiceTest {

    @Mock
    private ItemService itemService;

    @Mock
    private UserService userService;

    @Mock
    private ItemStatusService itemStatusService;

    @Mock
    private BidService bidService;

    @Mock
    private ItemPricesSink itemPricesSink;

    @InjectMocks
    private AuctionService auctionService;

    private User seller;
    private User bidder1;
    private User bidder2;
    private Item testItem;
    private ItemStatus testItemStatus;
    private static final Long ITEM_ID = 1L;

    @BeforeEach
    void setUp() {
        // Inject @Value field
        ReflectionTestUtils.setField(auctionService, "extraTime", 300000L); // 5 phút

        seller = new User("seller", "Seller Name", "hashedpw", 1000.0);
        bidder1 = new User("bidder1", "Bidder One", "hashedpw", 5000.0);
        bidder2 = new User("bidder2", "Bidder Two", "hashedpw", 5000.0);

        testItem = new Item(seller, "Test Item", "A test auction item");
        ReflectionTestUtils.setField(testItem, "itemId", ITEM_ID);

        testItemStatus = new ItemStatus();
        testItemStatus.setCurrentPrice(0.0);
        testItemStatus.setHighestBidUser("seller");
        testItemStatus.setItemStatus("ACTIVE");
        ReflectionTestUtils.setField(testItemStatus, "startingPrice", 1000.0);
        ReflectionTestUtils.setField(testItemStatus, "bidIncrement", 50.0);
        ReflectionTestUtils.setField(testItemStatus, "buyItNowPrice", 2000.0);
        ReflectionTestUtils.setField(testItemStatus, "endTime",
                Instant.now().toEpochMilli() + 3600000L); // 1 giờ từ bây giờ
        ReflectionTestUtils.setField(testItemStatus, "maxEndTime",
                Instant.now().toEpochMilli() + 7200000L); // 2 giờ max
    }

    /**
     * Thiết lập mock chung cho các validation cơ bản (auction chưa kết thúc).
     */
    private void setupBasicValidationMocks() {
        lenient().when(itemStatusService.auctionEndedOrNot(ITEM_ID)).thenReturn(false);
    }

    // ========================================================================
    // Nhóm test: createBid (Đặt cược thủ công)
    // ========================================================================
    @Nested
    @DisplayName("createBid - Đặt cược thủ công")
    class CreateBidTests {

        @Test
        @DisplayName("Đặt cược thành công lần đầu - Không có Auto-Bid")
        void createBid_FirstBid_Success() {
            // Arrange
            BidPostRequest request = new BidPostRequest(ITEM_ID, 1050.0);
            setupBasicValidationMocks();

            when(itemService.getItemRef(ITEM_ID)).thenReturn(testItem);
            when(userService.getUserRef("bidder1")).thenReturn(bidder1);
            when(itemStatusService.getItemStatus(ITEM_ID)).thenReturn(testItemStatus);
            when(bidService.existUserAndItem(bidder1, testItem)).thenReturn(false);
            when(bidService.getAutoBidByItemId(ITEM_ID)).thenReturn(Optional.empty());

            // Act
            BaseObjectResponse<Bid> response = auctionService.createBid(request, "bidder1");

            // Assert
            assertTrue(response.getStatus());
            assertEquals("Successfully created bid for an item", response.getMessage());
            verify(bidService).saveBid(any(Bid.class));
            verify(userService).deductBalance("bidder1", 1050.0);
        }

        @Test
        @DisplayName("Cập nhật lượt cược khi đã đặt trước đó")
        void createBid_ExistingBid_UpdatesAmount() {
            // Arrange
            BidPostRequest request = new BidPostRequest(ITEM_ID, 1100.0);
            Bid existingBid = new Bid(testItem, bidder1, 1050.0);
            setupBasicValidationMocks();

            when(itemService.getItemRef(ITEM_ID)).thenReturn(testItem);
            when(userService.getUserRef("bidder1")).thenReturn(bidder1);
            when(itemStatusService.getItemStatus(ITEM_ID)).thenReturn(testItemStatus);
            when(bidService.existUserAndItem(bidder1, testItem)).thenReturn(true);
            when(bidService.getBidByUserAndItem(bidder1, testItem)).thenReturn(existingBid);
            when(bidService.getAutoBidByItemId(ITEM_ID)).thenReturn(Optional.empty());

            // Act
            BaseObjectResponse<Bid> response = auctionService.createBid(request, "bidder1");

            // Assert
            assertTrue(response.getStatus());
            assertEquals(1100.0, existingBid.getBidAmount());
            verify(bidService).saveBid(existingBid);
        }

        @Test
        @DisplayName("Không cho phép tự đấu giá sản phẩm của mình")
        void createBid_SelfBid_ThrowsException() {
            // Arrange
            BidPostRequest request = new BidPostRequest(ITEM_ID, 1050.0);

            when(itemService.getItemRef(ITEM_ID)).thenReturn(testItem);
            when(userService.getUserRef("seller")).thenReturn(seller);
            when(itemStatusService.getItemStatus(ITEM_ID)).thenReturn(testItemStatus);

            // Act & Assert
            BaseException ex = assertThrows(BaseException.class,
                    () -> auctionService.createBid(request, "seller"));
            assertEquals("You can't place bid on your own item", ex.getMessage());
        }

        @Test
        @DisplayName("Không cho phép đặt giá thấp hơn giá khởi điểm")
        void createBid_BelowStartingPrice_ThrowsException() {
            // Arrange
            BidPostRequest request = new BidPostRequest(ITEM_ID, 500.0);

            when(itemService.getItemRef(ITEM_ID)).thenReturn(testItem);
            when(userService.getUserRef("bidder1")).thenReturn(bidder1);
            when(itemStatusService.getItemStatus(ITEM_ID)).thenReturn(testItemStatus);

            // Act & Assert
            BaseException ex = assertThrows(BaseException.class,
                    () -> auctionService.createBid(request, "bidder1"));
            assertEquals("Your bid must be higher than the starting price", ex.getMessage());
        }

        @Test
        @DisplayName("Không cho phép đặt giá khi phiên đấu giá đã kết thúc")
        void createBid_AuctionEnded_ThrowsException() {
            // Arrange
            BidPostRequest request = new BidPostRequest(ITEM_ID, 1050.0);

            when(itemService.getItemRef(ITEM_ID)).thenReturn(testItem);
            when(userService.getUserRef("bidder1")).thenReturn(bidder1);
            when(itemStatusService.getItemStatus(ITEM_ID)).thenReturn(testItemStatus);
            when(itemStatusService.auctionEndedOrNot(ITEM_ID)).thenReturn(true);

            // Act & Assert
            BaseException ex = assertThrows(BaseException.class,
                    () -> auctionService.createBid(request, "bidder1"));
            assertEquals("Auction has already ended", ex.getMessage());
        }

        @Test
        @DisplayName("Không cho phép đặt khi số dư không đủ")
        void createBid_InsufficientBalance_ThrowsException() {
            // Arrange
            User poorBidder = new User("poor", "Poor User", "hashedpw", 100.0);
            BidPostRequest request = new BidPostRequest(ITEM_ID, 1050.0);
            setupBasicValidationMocks();

            when(itemService.getItemRef(ITEM_ID)).thenReturn(testItem);
            when(userService.getUserRef("poor")).thenReturn(poorBidder);
            when(itemStatusService.getItemStatus(ITEM_ID)).thenReturn(testItemStatus);

            // Act & Assert
            BaseException ex = assertThrows(BaseException.class,
                    () -> auctionService.createBid(request, "poor"));
            assertEquals("You don't have enough money", ex.getMessage());
        }

        @Test
        @DisplayName("Không cho phép đặt thấp hơn mức giá hiện tại + bước giá")
        void createBid_BelowMinimumIncrement_ThrowsException() {
            // Arrange: Giá hiện tại = 1050, bước giá = 50 -> cần >= 1100
            testItemStatus.setCurrentPrice(1050.0);
            testItemStatus.setHighestBidUser("bidder2");
            BidPostRequest request = new BidPostRequest(ITEM_ID, 1080.0);
            setupBasicValidationMocks();

            when(itemService.getItemRef(ITEM_ID)).thenReturn(testItem);
            when(userService.getUserRef("bidder1")).thenReturn(bidder1);
            when(itemStatusService.getItemStatus(ITEM_ID)).thenReturn(testItemStatus);

            // Act & Assert
            BaseException ex = assertThrows(BaseException.class,
                    () -> auctionService.createBid(request, "bidder1"));
            assertEquals("Your bid must be higher than the current highest", ex.getMessage());
        }

        @Test
        @DisplayName("Hoàn tiền cho người dẫn đầu cũ khi bị outbid")
        void createBid_RefundsPreviousHighestBidder() {
            // Arrange: bidder2 đang dẫn đầu ở mức 1050
            testItemStatus.setCurrentPrice(1050.0);
            testItemStatus.setHighestBidUser("bidder2");
            BidPostRequest request = new BidPostRequest(ITEM_ID, 1100.0);
            setupBasicValidationMocks();

            when(itemService.getItemRef(ITEM_ID)).thenReturn(testItem);
            when(userService.getUserRef("bidder1")).thenReturn(bidder1);
            when(itemStatusService.getItemStatus(ITEM_ID)).thenReturn(testItemStatus);
            when(bidService.existUserAndItem(bidder1, testItem)).thenReturn(false);
            when(bidService.getAutoBidByItemId(ITEM_ID)).thenReturn(Optional.empty());

            // Act
            auctionService.createBid(request, "bidder1");

            // Assert: bidder2 được hoàn 1050, bidder1 bị trừ 1100
            verify(userService).addBalance("bidder2", 1050.0);
            verify(userService).deductBalance("bidder1", 1100.0);
        }
    }

    // ========================================================================
    // Nhóm test: buyItemNow (Mua ngay)
    // ========================================================================
    @Nested
    @DisplayName("buyItemNow - Mua đứt sản phẩm")
    class BuyItemNowTests {

        @Test
        @DisplayName("Mua ngay thành công - Kết thúc phiên đấu giá")
        void buyItemNow_Success() {
            // Arrange
            testItemStatus.setCurrentPrice(1050.0);
            testItemStatus.setHighestBidUser("bidder2");
            setupBasicValidationMocks();

            when(itemStatusService.getItemStatus(ITEM_ID)).thenReturn(testItemStatus);
            when(userService.getUserByUsername("bidder1")).thenReturn(bidder1);
            when(itemService.getItem(ITEM_ID)).thenReturn(testItem);

            // Act
            BaseResponse response = auctionService.buyItemNow(ITEM_ID, "bidder1");

            // Assert
            assertTrue(response.getStatus());
            assertEquals("Successfully bought item", response.getMessage());
            verify(userService).addBalance("bidder2", 1050.0);
            verify(userService).deductBalance("bidder1", 2000.0);
            verify(itemPricesSink).publishPrice(ITEM_ID, 2000.0);
            verify(itemStatusService).saveStatus(testItemStatus);
        }

        @Test
        @DisplayName("Mua ngay thất bại - Người bán không thể tự mua")
        void buyItemNow_SellerCannotBuy() {
            // Arrange
            when(itemStatusService.getItemStatus(ITEM_ID)).thenReturn(testItemStatus);
            when(userService.getUserByUsername("seller")).thenReturn(seller);
            when(itemService.getItem(ITEM_ID)).thenReturn(testItem);

            // Act & Assert
            BaseException ex = assertThrows(BaseException.class,
                    () -> auctionService.buyItemNow(ITEM_ID, "seller"));
            assertEquals("You can't place bid on your own item", ex.getMessage());
        }
    }

    // ========================================================================
    // Nhóm test: createAutoBid (Tự động đặt cược)
    // ========================================================================
    @Nested
    @DisplayName("createAutoBid - Cấu hình tự động đấu giá")
    class CreateAutoBidTests {

        @Test
        @DisplayName("Tạo Auto-Bid mới khi chưa có ai cài đặt trước")
        void createAutoBid_NewAutoBid_Success() {
            // Arrange
            AutoBidRequest request = new AutoBidRequest(ITEM_ID, 1500.0);
            setupBasicValidationMocks();

            when(userService.getUserByUsername("bidder1")).thenReturn(bidder1);
            when(bidService.getAutoBidByItemId(ITEM_ID)).thenReturn(Optional.empty());
            when(itemStatusService.getItemStatus(ITEM_ID)).thenReturn(testItemStatus);
            when(itemService.getItem(ITEM_ID)).thenReturn(testItem);

            // Act
            BaseResponse response = auctionService.createAutoBid(request, "bidder1");

            // Assert
            assertTrue(response.getStatus());
            verify(userService).deductBalance("bidder1", 1500.0);
            verify(bidService).saveAutoBid(any(AutoBid.class));
        }

        @Test
        @DisplayName("Cùng người cập nhật tăng hạn mức Auto-Bid")
        void createAutoBid_SameUser_IncreasesLimit() {
            // Arrange
            AutoBid existingAutoBid = new AutoBid(ITEM_ID, bidder1, 1500.0, 1050.0);
            AutoBidRequest request = new AutoBidRequest(ITEM_ID, 2000.0);
            setupBasicValidationMocks();

            when(userService.getUserByUsername("bidder1")).thenReturn(bidder1);
            when(bidService.getAutoBidByItemId(ITEM_ID)).thenReturn(Optional.of(existingAutoBid));
            when(itemStatusService.getItemStatus(ITEM_ID)).thenReturn(testItemStatus);
            when(itemService.getItem(ITEM_ID)).thenReturn(testItem);

            // Act
            BaseResponse response = auctionService.createAutoBid(request, "bidder1");

            // Assert
            assertTrue(response.getStatus());
            verify(userService).deductBalance("bidder1", 500.0); // 2000 - 1500
            assertEquals(2000.0, existingAutoBid.getMaxBidLimit());
        }

        @Test
        @DisplayName("Cùng người giảm hạn mức Auto-Bid → hoàn tiền chênh lệch")
        void createAutoBid_SameUser_DecreasesLimit() {
            // Arrange
            AutoBid existingAutoBid = new AutoBid(ITEM_ID, bidder1, 1500.0, 1050.0);
            AutoBidRequest request = new AutoBidRequest(ITEM_ID, 1200.0);
            setupBasicValidationMocks();

            when(userService.getUserByUsername("bidder1")).thenReturn(bidder1);
            when(bidService.getAutoBidByItemId(ITEM_ID)).thenReturn(Optional.of(existingAutoBid));
            when(itemStatusService.getItemStatus(ITEM_ID)).thenReturn(testItemStatus);
            when(itemService.getItem(ITEM_ID)).thenReturn(testItem);

            // Act
            BaseResponse response = auctionService.createAutoBid(request, "bidder1");

            // Assert
            assertTrue(response.getStatus());
            verify(userService).addBalance("bidder1", 300.0); // 1500 - 1200
            assertEquals(1200.0, existingAutoBid.getMaxBidLimit());
        }

        @Test
        @DisplayName("Người mới thắng Auto-Bid khi hạn mức cao hơn đối thủ")
        void createAutoBid_NewUserWins_HigherLimit() {
            // Arrange
            AutoBid existingAutoBid = new AutoBid(ITEM_ID, bidder1, 1300.0, 1050.0);
            AutoBidRequest request = new AutoBidRequest(ITEM_ID, 1500.0);
            setupBasicValidationMocks();

            when(userService.getUserByUsername("bidder2")).thenReturn(bidder2);
            when(bidService.getAutoBidByItemId(ITEM_ID)).thenReturn(Optional.of(existingAutoBid));
            when(itemStatusService.getItemStatus(ITEM_ID)).thenReturn(testItemStatus);
            when(itemService.getItem(ITEM_ID)).thenReturn(testItem);

            // Act
            BaseResponse response = auctionService.createAutoBid(request, "bidder2");

            // Assert
            assertTrue(response.getStatus());
            verify(userService).addBalance("bidder1", 1300.0);
            verify(userService).deductBalance("bidder2", 1500.0);
        }
    }

    // ========================================================================
    // Nhóm test: getMyCurrentBids & getMyWinnings
    // ========================================================================
    @Nested
    @DisplayName("Query methods - Truy vấn dữ liệu")
    class QueryTests {

        @Test
        @DisplayName("Lấy danh sách cược của bản thân thành công")
        void getMyCurrentBids_Success() {
            // Arrange
            List<Bid> bidList = List.of(new Bid(testItem, bidder1, 1050.0));
            Page<Bid> bidPage = new PageImpl<>(bidList);

            when(userService.getUserRef("bidder1")).thenReturn(bidder1);
            when(bidService.getAllUserBid(eq(bidder1), any(PageRequest.class))).thenReturn(bidPage);

            // Act
            BaseObjectResponse<Page<Bid>> response =
                    auctionService.getMyCurrentBids("bidder1", 0, 10);

            // Assert
            assertTrue(response.getStatus());
            assertEquals(1, response.getEntity().getTotalElements());
        }

        @Test
        @DisplayName("Lấy danh sách thắng cuộc thành công")
        void getMyWinnings_Success() {
            // Arrange
            Bid winBid = new Bid(testItem, bidder1, 1500.0);
            when(bidService.getUserWins("bidder1")).thenReturn(List.of(winBid));

            // Act
            BaseObjectResponse<List<BidAndItem>> response =
                    auctionService.getMyWinnings("bidder1");

            // Assert
            assertTrue(response.getStatus());
            assertEquals(1, response.getEntity().size());
            assertEquals(testItem, response.getEntity().get(0).getItem());
        }

        @Test
        @DisplayName("Danh sách thắng cuộc rỗng khi chưa thắng gì")
        void getMyWinnings_Empty() {
            // Arrange
            when(bidService.getUserWins("bidder1")).thenReturn(new ArrayList<>());

            // Act
            BaseObjectResponse<List<BidAndItem>> response =
                    auctionService.getMyWinnings("bidder1");

            // Assert
            assertTrue(response.getStatus());
            assertEquals(0, response.getEntity().size());
        }
    }
}
