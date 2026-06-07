package com.auction.items;

import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;
import com.auction.items.dto.PublishItemRequest;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserService;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {
  private final ItemRepository itemRepository;
  private final UserService userService;
  private final ItemStatusService itemStatusService;

  @Value("${max_extra_time}")
  private Long maxExtraTime;

  public ItemService(
      ItemRepository itemRepository, UserService userService, ItemStatusService itemStatusService) {
    this.itemRepository = itemRepository;
    this.userService = userService;
    this.itemStatusService = itemStatusService;
  }

  /**
   * Thực hiện nghiệp vụ đăng sản phẩm mới lên sàn đấu giá.
   *
   * @param request Dữ liệu sản phẩm đăng đấu giá
   * @param username Tên đăng nhập của người đăng bán
   * @return BaseObjectResponse chứa sản phẩm vừa được tạo
   */
  @Transactional
  public BaseObjectResponse<Item> publishItem(PublishItemRequest request, String username) {
    // Lấy tham chiếu người dùng từ UserService
    User user = userService.getUserReferenceByUsername(username);
    // Lưu thông tin sản phẩm vào DB
    Item item = saveItem(new Item(user, request.title(), request.description()));

    // Kiểm tra tính hợp lệ của thời gian kết thúc (phải lớn hơn thời gian hiện tại)
    if (request.endTime() < Instant.now().toEpochMilli()) {
      throw new BaseException("Your end time must be higher than the current time");
    }

    // Khởi tạo trạng thái ban đầu của sản phẩm đấu giá (ItemStatus) và lưu vào DB
    itemStatusService.saveStatus(
        new ItemStatus(
            item,
            0.0,
            username,
            request.endTime(),
            request.startingPrice(),
            request.buyItNowPrice(),
            request.bidIncrement(),
            request.endTime() + maxExtraTime));

    return new BaseObjectResponse<>(true, "Created new item.", item);
  }

  /** Lấy phản hồi thông tin sản phẩm dựa trên ID. */
  @Transactional(readOnly = true)
  public BaseObjectResponse<Item> getItemRes(Long itemId) {
    Item item =
        itemRepository
            .findById(itemId)
            .orElseThrow(() -> new BaseException("This Item Id does not exist"));
    return new BaseObjectResponse<>(true, "Successfully get Item", item);
  }

  /** Lấy toàn bộ danh sách sản phẩm (hỗ trợ cho API phát triển). */
  @Transactional(readOnly = true)
  public BaseObjectResponse<List<Item>> getItems() {
    List<Item> items = itemRepository.findAll();
    return new BaseObjectResponse<>(true, "Successfully get all items", items);
  }

  /** Lấy danh sách sản phẩm đấu giá đang hoạt động (phân trang và sắp xếp theo tiêu đề). */
  @Transactional(readOnly = true)
  public BaseObjectResponse<Page<Item>> getActiveItemsByPageTitle(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("item.title"));
    Page<Item> pages = itemRepository.findActiveItemPage(pageable, Instant.now().toEpochMilli());
    return new BaseObjectResponse<>(true, "successfully got pages", pages);
  }

  /** Lấy danh sách các sản phẩm mà một người dùng cụ thể đang đăng bán (có phân trang). */
  @Transactional(readOnly = true)
  public BaseObjectResponse<Page<Item>> getListingByUser(int page, int size, String username) {
    Pageable pageable = PageRequest.of(page, size);
    User userRef = userService.getUserRef(username);
    Page<Item> items = itemRepository.findItemByUser(pageable, userRef);
    return new BaseObjectResponse<Page<Item>>(true, "succesfully got listing", items);
  }

  /** Kiểm tra sự tồn tại của sản phẩm theo mã ID. */
  @Transactional
  public boolean existByItemId(Long itemId) {
    return itemRepository.existsById(itemId);
  }

  /** Lấy tham chiếu Proxy của sản phẩm mà không cần tải dữ liệu từ DB ngay lập tức. */
  @Transactional
  public Item getItemRef(Long itemId) {
    return itemRepository.getReferenceById(itemId);
  }

  /** Lấy chi tiết thông tin thực thể Item theo ID. Ném ngoại lệ nếu không tìm thấy. */
  @Transactional
  public Item getItem(Long itemId) {
    return itemRepository
        .findById(itemId)
        .orElseThrow(() -> new BaseException("There is no such Item with that ID"));
  }

  /** Lưu thực thể Item vào cơ sở dữ liệu. */
  @Transactional
  public Item saveItem(Item item) {
    item = itemRepository.save(item);
    return item;
  }
}
