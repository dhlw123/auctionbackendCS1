package com.auction.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auction.common.BaseObjectResponse;
import com.auction.items.dto.PublishItemRequest;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

  @Mock private ItemRepository itemRepository;

  @Mock private UserService userService;

  @Mock private ItemStatusService itemStatusService;

  @InjectMocks private ItemService itemService;

  private User testUser;
  private Item testItem;

  @BeforeEach
  void setUp() {
    testUser = new User("testuser", "Test User", "hashedpassword", 0.0);

    testItem = new Item();
    testItem.setUser(testUser);

    ReflectionTestUtils.setField(itemService, "maxExtraTime", 3600000L);
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
}
