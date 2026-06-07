package com.auction.items;

import com.auction.auctionorchestration.AuctionService;
import com.auction.auth.jwtools.UserDetailsImpl;
import com.auction.common.BaseObjectResponse;
import com.auction.common.BaseResponse;
import com.auction.items.dto.PublishItemRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/items")
public class ItemController {
  private final ItemService itemService;
  private final AuctionService auctionService;

  public ItemController(ItemService itemService, AuctionService auctionService) {
    this.itemService = itemService;
    this.auctionService = auctionService;
  }

  /**
   * API tạo mới và đăng bán một sản phẩm đấu giá. POST /items
   *
   * @param userDetailsImpl Thông tin người dùng hiện tại đang đăng nhập
   * @param request Dữ liệu đăng bán sản phẩm (tiêu đề, giá, bước giá, thời gian)
   * @return ResponseEntity chứa thông tin sản phẩm vừa tạo
   */
  @PostMapping("")
  public ResponseEntity<BaseObjectResponse<Item>> postItem(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
      @Valid @RequestBody PublishItemRequest request) {
    BaseObjectResponse<Item> response =
        itemService.publishItem(request, userDetailsImpl.getUsername());
    return ResponseEntity.ok().body(response);
  }

  /**
   * API hủy bỏ một phiên đấu giá sản phẩm. Chỉ người bán mới có quyền thực hiện. POST
   * /items/cancel/{itemId}
   *
   * @param itemId Mã sản phẩm cần hủy
   * @param userDetailsImpl Thông tin người dùng hiện tại đang đăng nhập
   * @return ResponseEntity phản hồi trạng thái hủy
   */
  @PostMapping("/cancel/{itemId}")
  public ResponseEntity<BaseResponse> cancelItem(
      @PathVariable Long itemId, @AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
    BaseResponse response = auctionService.cancelItem(itemId, userDetailsImpl.getUsername());
    return ResponseEntity.ok().body(response);
  }

  /**
   * API lấy chi tiết thông tin của một sản phẩm qua mã ID. GET /items/{itemId}
   *
   * @param itemId Mã sản phẩm cần tìm
   * @return ResponseEntity chứa thông tin chi tiết của sản phẩm
   */
  @GetMapping("/{itemId}")
  public ResponseEntity<BaseObjectResponse<Item>> getItem(@PathVariable Long itemId) {
    BaseObjectResponse<Item> response = itemService.getItemRes(itemId);
    return ResponseEntity.ok().body(response);
  }

  /**
   * API lấy danh sách toàn bộ sản phẩm trên hệ thống. LƯU Ý: API này chỉ được sử dụng cho môi
   * trường phát triển (development). Hãy xóa bỏ ở production. GET /items/all
   *
   * @return ResponseEntity chứa danh sách toàn bộ sản phẩm
   */
  @GetMapping("/all")
  public ResponseEntity<BaseObjectResponse<List<Item>>> getItems() {
    BaseObjectResponse<List<Item>> response = itemService.getItems();
    return ResponseEntity.ok().body(response);
  }

  /**
   * API lấy danh sách các sản phẩm đang có phiên đấu giá hoạt động (chưa kết thúc), hỗ trợ phân
   * trang. GET /items?page=0&size=10
   *
   * @param page Trang hiện tại cần tải (bắt đầu từ 0)
   * @param size Số lượng bản ghi mỗi trang (tối thiểu 1, tối đa 20, mặc định 10)
   * @return ResponseEntity chứa danh sách phân trang các sản phẩm đang hoạt động
   */
  @GetMapping("")
  public ResponseEntity<BaseObjectResponse<Page<Item>>> getActiveItems(
      @Min(0) @RequestParam(defaultValue = "0") int page,
      @Min(1) @Max(20) @RequestParam(defaultValue = "10") int size) {
    BaseObjectResponse<Page<Item>> request = itemService.getActiveItemsByPageTitle(page, size);
    return ResponseEntity.ok().body(request);
  }

  /**
   * API lấy danh sách các sản phẩm do một người dùng cụ thể đăng bán, hỗ trợ phân trang. GET
   * /items/listings/{username}?page=0&size=10
   *
   * @param username Tên đăng nhập của người bán
   * @param page Trang cần tải
   * @param size Kích thước trang
   * @return ResponseEntity chứa danh sách phân trang các sản phẩm của người dùng đó
   */
  @GetMapping("/listings/{username}")
  public ResponseEntity<BaseObjectResponse<Page<Item>>> getListing(
      @PathVariable String username,
      @Min(0) @RequestParam(defaultValue = "0") int page,
      @Min(1) @Max(20) @RequestParam(defaultValue = "10") int size) {
    BaseObjectResponse<Page<Item>> request = itemService.getListingByUser(page, size, username);
    return ResponseEntity.ok().body(request);
  }
}
