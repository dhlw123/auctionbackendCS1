package com.auction.bids;

import com.auction.common.BaseObjectResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller chịu trách nhiệm xử lý các yêu cầu API liên quan đến lịch sử đặt giá cược (Bids). */
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/bids")
public class BidController {
  private final BidService bidService;

  public BidController(BidService bidService) {
    this.bidService = bidService;
  }

  /**
   * API truy vấn phân trang danh sách lịch sử đặt giá (bids) của một sản phẩm cụ thể. GET
   * /bids/{itemId}/bids
   *
   * @param itemId Mã ID của sản phẩm cần lấy lịch sử đấu giá
   * @param page Số trang hiện tại cần lấy (bắt đầu từ 0)
   * @param size Số lượng bản ghi mỗi trang (tối thiểu 1, tối đa 20, mặc định 10)
   * @return ResponseEntity chứa danh sách phân trang lịch sử đặt giá
   */
  @GetMapping("/{itemId}/bids")
  public ResponseEntity<BaseObjectResponse<Page<Bid>>> getBids(
      @PathVariable Long itemId,
      @Min(0) @RequestParam(defaultValue = "0") int page,
      @Min(1) @Max(20) @RequestParam(defaultValue = "10") int size) {
    BaseObjectResponse<Page<Bid>> response = bidService.getBidsOnItem(itemId, page, size);
    return ResponseEntity.ok().body(response);
  }
}
