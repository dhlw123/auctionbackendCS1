package com.auction.itemstatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auction.common.BaseObjectResponse;
import com.auction.items.Item;
import com.auction.users.User;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemStatusServiceTest {

  @Mock private ItemStatusRepository itemStatusRepository;

  @InjectMocks private ItemStatusService itemStatusService;

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
    testItemStatus.setEndTime(Instant.now().toEpochMilli() + 100000L); // Future end time
  }

  @Test
  void saveStatus_Success() {
    // Arrange
    when(itemStatusRepository.save(any(ItemStatus.class))).thenReturn(testItemStatus);

    // Act
    ItemStatus result = itemStatusService.saveStatus(testItemStatus);

    // Assert
    assertEquals(testItemStatus, result);
    verify(itemStatusRepository).save(testItemStatus);
  }

  @Test
  void updateStatus_Success() {
    // Arrange
    when(itemStatusRepository.findByItemWithLock(testItem)).thenReturn(testItemStatus);
    when(itemStatusRepository.save(any(ItemStatus.class))).thenReturn(testItemStatus);

    // Act
    ItemStatus result = itemStatusService.updateStatus(testItem, 20.0, "newbidder");

    // Assert
    assertEquals(20.0, result.getCurrentPrice());
    assertEquals("newbidder", result.getHighestBidUser());
    verify(itemStatusRepository).save(testItemStatus);
  }

  @Test
  void getStatusResponse_Success() {
    // Arrange
    Long itemId = 1L;
    when(itemStatusRepository.findByItemWithLockByItemId(itemId)).thenReturn(testItemStatus);

    // Act
    BaseObjectResponse<ItemStatus> response = itemStatusService.getStatusResponse(itemId);

    // Assert
    assertTrue(response.getStatus());
    assertEquals("Succesfully get item status", response.getMessage());
    assertEquals(testItemStatus, response.getEntity());
  }

  @Test
  void getItemStatus_Success() {
    // Arrange
    Long itemId = 1L;
    when(itemStatusRepository.findByItemWithLockByItemId(itemId)).thenReturn(testItemStatus);

    // Act
    ItemStatus result = itemStatusService.getItemStatus(itemId);

    // Assert
    assertEquals(testItemStatus, result);
  }

  @Test
  void getAllItemStatus_Success() {
    // Arrange
    when(itemStatusRepository.findAll()).thenReturn(List.of(testItemStatus));

    // Act
    List<ItemStatus> result = itemStatusService.getAllItemStatus();

    // Assert
    assertEquals(1, result.size());
    assertEquals(testItemStatus, result.get(0));
  }
}
