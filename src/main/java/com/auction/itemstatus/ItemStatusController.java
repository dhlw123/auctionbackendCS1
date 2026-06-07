package com.auction.itemstatus;

import com.auction.common.BaseObjectResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller xử lý các yêu cầu lấy thông tin trạng thái và tiến trình đấu giá của các mặt hàng. */
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/item")
public class ItemStatusController {
  private final ItemStatusService itemStatusService;

  public ItemStatusController(ItemStatusService itemStatusService) {
    this.itemStatusService = itemStatusService;
  }

  /**
   * API truy vấn trạng thái đấu giá hiện tại của một mặt hàng cụ thể. GET /item/status/{itemId}
   *
   * @param itemId Mã ID sản phẩm
   * @return ResponseEntity chứa thông tin trạng thái (giá hiện tại, người giữ giá, thời gian kết
   *     thúc)
   */
  @GetMapping("/status/{itemId}")
  public ResponseEntity<BaseObjectResponse<ItemStatus>> getItemStatus(@PathVariable Long itemId) {
    BaseObjectResponse<ItemStatus> response = itemStatusService.getStatusResponse(itemId);
    return ResponseEntity.ok().body(response);
  }

  /**
   * API lấy danh sách toàn bộ trạng thái sản phẩm đấu giá trên hệ thống. Chỉ sử dụng cho mục đích
   * phát triển (Development API). GET /item/status/all
   *
   * @return Danh sách toàn bộ thực thể ItemStatus
   */
  @GetMapping("status/all")
  public ResponseEntity<List<ItemStatus>> getItemStatuses() {
    return ResponseEntity.ok(itemStatusService.getAllItemStatus());
  }
}
