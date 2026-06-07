package com.auction.auctionorchestration.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auction.items.Item;
import com.auction.items.ItemService;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BidValidatorTest {

  @Mock private ItemStatusService itemStatusService;

  @Mock private ItemService itemService;

  @Mock private UserService userService;

  @InjectMocks private BidValidator bidValidator;

  private Item testItem;
  private ItemStatus testItemStatus;

  @BeforeEach
  void setUp() {
    User user = new User("testuser", "Test User", "password", 100.0);
    testItem = new Item(user, "Test Item", "Description");

    testItemStatus = new ItemStatus();
    testItemStatus.setItem(testItem);
    testItemStatus.setCurrentPrice(10.0);
    testItemStatus.setItemStatus("ACTIVE");
    testItemStatus.setEndTime(Instant.now().toEpochMilli() + 100000L);
  }

  @Test
  void auctionEndedOrNot_AlreadyEnded_ReturnsTrue() {
    Long itemId = 1L;
    testItemStatus.setItemStatus("ENDED");
    when(itemStatusService.getItemStatus(itemId)).thenReturn(testItemStatus);

    boolean result = bidValidator.auctionEndedOrNot(itemId);

    assertTrue(result);
  }

  @Test
  void auctionEndedOrNot_AlreadyCanceled_ReturnsTrue() {
    Long itemId = 1L;
    testItemStatus.setItemStatus("CANCELED");
    when(itemStatusService.getItemStatus(itemId)).thenReturn(testItemStatus);

    boolean result = bidValidator.auctionEndedOrNot(itemId);

    assertTrue(result);
  }

  @Test
  void auctionEndedOrNot_TimeExpired_UpdatesStatusAndReturnsTrue() {
    Long itemId = 1L;
    testItemStatus.setEndTime(Instant.now().toEpochMilli() - 100000L);
    when(itemStatusService.getItemStatus(itemId)).thenReturn(testItemStatus);
    when(itemService.getItem(itemId)).thenReturn(testItem);

    boolean result = bidValidator.auctionEndedOrNot(itemId);

    assertTrue(result);
    assertEquals("ENDED", testItemStatus.getItemStatus());
    verify(itemStatusService).saveStatus(testItemStatus);
    verify(userService).addBalance("testuser", 10.0);
  }

  @Test
  void auctionEndedOrNot_ActiveAndNotExpired_ReturnsFalse() {
    Long itemId = 1L;
    when(itemStatusService.getItemStatus(itemId)).thenReturn(testItemStatus);

    boolean result = bidValidator.auctionEndedOrNot(itemId);

    assertFalse(result);
  }
}
