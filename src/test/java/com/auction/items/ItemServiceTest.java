package com.auction.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auction.auctionorchestration.helper.BidValidator;
import com.auction.bids.BidRepository;
import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;
import com.auction.common.BaseResponse;
import com.auction.items.dto.PublishItemRequest;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

  @Mock private ItemRepository itemRepository;

  @Mock private UserService userService;

  @Mock private ItemStatusService itemStatusService;

  @Mock private BidRepository bidRepository;

  @Mock private BidValidator bidValidator;

  @InjectMocks private ItemService itemService;

  private User testUser;
  private Item testItem;
  private ItemStatus testItemStatus;

  @BeforeEach
  void setUp() {
    testUser = new User("testuser", "Test User", "hashedpassword", 0.0);

    testItem = new Item();
    testItem.setUser(testUser);

    testItemStatus = new ItemStatus();
    testItemStatus.setItemStatus("ACTIVE");
    testItemStatus.setHighestBidUser("anotheruser");
    testItemStatus.setCurrentPrice(50.0);

    // Set the @Value field for maxExtraTime since it's null in unit tests
    ReflectionTestUtils.setField(itemService, "maxExtraTime", 3600000L); // 1 hour in ms
  }

  @Test
  void publishItem_Success() {
    // Arrange
    long futureEndTime = Instant.now().toEpochMilli() + 100000;
    PublishItemRequest request =
        new PublishItemRequest("Test Item", "Description", futureEndTime, 10.0, 100.0, 5.0);
    String username = "testuser";

    when(userService.getUserReferenceByUsername(username)).thenReturn(testUser);
    when(itemRepository.save(any(Item.class))).thenReturn(testItem);

    // Act
    BaseObjectResponse<Item> response = itemService.publishItem(request, username);

    // Assert
    assertEquals(true, response.getStatus());
    assertEquals("Created new item.", response.getMessage());
    assertNotNull(response.getEntity());
    verify(itemStatusService).saveStatus(any(ItemStatus.class));
  }

  @Test
  void cancelItem_Success_RefundsHighestBidder() {
    // Arrange
    Long itemId = 1L;
    String username = "testuser";

    when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
    when(itemStatusService.getItemStatus(itemId)).thenReturn(testItemStatus);
    when(bidValidator.auctionEndedOrNot(itemId)).thenReturn(false);

    // Act
    BaseResponse response = itemService.cancelItem(itemId, username);

    // Assert
    assertEquals(true, response.getStatus());
    assertEquals("Item successfully canceled.", response.getMessage());
    assertEquals("CANCELED", testItemStatus.getItemStatus());
    verify(itemStatusService).saveStatus(testItemStatus);
    // Verify that the highest bidder was refunded
    verify(userService).addBalance("anotheruser", 50.0);
  }

  @Test
  void cancelItem_ItemNotFound_ThrowsException() {
    // Arrange
    Long itemId = 1L;
    String username = "testuser";

    when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

    // Act & Assert
    BaseException exception =
        assertThrows(
            BaseException.class,
            () -> {
              itemService.cancelItem(itemId, username);
            });
    assertEquals("There is no such Item with that ID", exception.getMessage());
  }

  @Test
  void cancelItem_UserNotOwner_ThrowsException() {
    // Arrange
    Long itemId = 1L;
    String username = "wronguser";

    when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));

    // Act & Assert
    BaseException exception =
        assertThrows(
            BaseException.class,
            () -> {
              itemService.cancelItem(itemId, username);
            });
    assertEquals("You are not the owner of this item", exception.getMessage());
  }

  @Test
  void cancelItem_NotActive_ThrowsException() {
    // Arrange
    Long itemId = 1L;
    String username = "testuser";
    testItemStatus.setItemStatus("ENDED");

    when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
    when(itemStatusService.getItemStatus(itemId)).thenReturn(testItemStatus);

    lenient().when(bidValidator.auctionEndedOrNot(itemId)).thenReturn(false);

    // Act & Assert
    BaseException exception =
        assertThrows(
            BaseException.class,
            () -> {
              itemService.cancelItem(itemId, username);
            });
    assertEquals("Only ACTIVE items can be canceled.", exception.getMessage());
  }
}
