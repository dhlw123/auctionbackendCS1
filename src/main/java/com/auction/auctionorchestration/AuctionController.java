package com.auction.auctionorchestration;

import com.auction.auctionorchestration.dto.AutoBidRequest;
import com.auction.auctionorchestration.dto.BidPostRequest;
import com.auction.auth.jwtools.UserDetailsImpl;
import com.auction.bids.Bid;
import com.auction.common.BaseObjectResponse;
import com.auction.common.BaseResponse;
import com.auction.common.jointdata.BidAndItem;
import com.auction.items.ItemPricesSink;
import com.auction.users.UserBalanceSink;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Controller điều phối toàn bộ luồng hoạt động của sàn đấu giá (Auction Orchestration).
 * Bao gồm đặt cược thủ công, tự động đặt cược, mua ngay, và phát trực tuyến (stream) giá/số dư theo thời gian thực.
 */
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping
public class AuctionController {

    private final AuctionService auctionService;
    private final ItemPricesSink itemsPricesSinks;
    private final UserBalanceSink userBalanceSink;

    public AuctionController(
            AuctionService auctionService,
            ItemPricesSink itemPricesSink,
            UserBalanceSink userBalanceSink
    ) {
        this.auctionService = auctionService;
        this.itemsPricesSinks = itemPricesSink;
        this.userBalanceSink = userBalanceSink;
    }

    /**
     * API thực hiện đặt giá cược (bid) thủ công cho một sản phẩm.
     * POST /bid
     *
     * @param userDetailsImpl Thông tin người đặt cược đã được xác thực
     * @param request         Dữ liệu lượt đặt cược gồm ID sản phẩm và số tiền cược
     * @return ResponseEntity chứa thông tin lượt cược được tạo thành công
     */
    @PostMapping("/bid")
    public ResponseEntity<BaseObjectResponse<Bid>> makeBid(
            @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
            @Valid @RequestBody BidPostRequest request
    ) {
        BaseObjectResponse<Bid> response = auctionService.createBid(
                request,
                userDetailsImpl.getUsername()
        );
        return ResponseEntity.ok().body(response);
    }

    /**
     * API lấy phân trang danh sách lịch sử cược của người dùng hiện tại đang đăng nhập.
     * GET /me/bids
     *
     * @param userDetailsImpl Thông tin người dùng hiện tại
     * @param page            Trang hiện tại cần lấy
     * @param size            Số lượng bản ghi mỗi trang
     * @return ResponseEntity chứa danh sách phân trang các lượt cược của người dùng
     */
    @GetMapping("/me/bids")
    public ResponseEntity<BaseObjectResponse<Page<Bid>>> bids(
            @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(20) @RequestParam(defaultValue = "10") int size
    ) {
        BaseObjectResponse<Page<Bid>> response =
                auctionService.getMyCurrentBids(
                        userDetailsImpl.getUsername(),
                        page,
                        size
                );
        return ResponseEntity.ok().body(response);
    }

    /**
     * API lấy toàn bộ danh sách các sản phẩm đấu giá mà người dùng hiện tại đã thắng cuộc.
     * GET /me/wins
     *
     * @param userDetailsImpl Thông tin người dùng hiện tại
     * @return ResponseEntity chứa danh sách các lượt đặt cược thắng kèm sản phẩm tương ứng
     */
    @GetMapping("/me/wins")
    public ResponseEntity<BaseObjectResponse<List<BidAndItem>>> getWins(
            @AuthenticationPrincipal UserDetailsImpl userDetailsImpl
    ) {
        BaseObjectResponse<List<BidAndItem>> response =
                auctionService.getMyWinnings(userDetailsImpl.getUsername());
        return ResponseEntity.ok().body(response);
    }

    /**
     * API Server-Sent Events (SSE) phát trực tuyến biến động giá của sản phẩm cho Client theo thời gian thực.
     * GET /items/stream/{itemId}
     *
     * @param itemId Mã ID sản phẩm cần theo dõi giá
     * @return Flux phát liên tục các mức giá mới (Double) dưới dạng Text Event Stream
     */
    @GetMapping(
            value = "/items/stream/{itemId}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<Double> streamPrice(@PathVariable Long itemId) {
        return itemsPricesSinks.getPriceSink(itemId);
    }

    /**
     * API Server-Sent Events (SSE) phát trực tuyến biến động số dư tài khoản của người dùng theo thời gian thực.
     * GET /{username}/balance/stream
     *
     * @param username Tên đăng nhập người dùng cần theo dõi số dư
     * @return Flux phát liên tục số dư mới (Double) khi có thay đổi tài chính
     */
    @GetMapping(
            value = "/{username}/balance/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<Double> streamBalance(@PathVariable String username) {
        return userBalanceSink.getBalanceSink(username);
    }

    /**
     * API thực hiện mua đứt sản phẩm đấu giá ngay lập tức theo giá "Buy It Now".
     * Kết thúc phiên đấu giá sản phẩm này ngay sau khi thực hiện thành công.
     * POST /buy-now/{itemId}
     *
     * @param userDetailsImpl Thông tin người mua
     * @param itemId          Mã ID sản phẩm muốn mua ngay
     * @return ResponseEntity phản hồi trạng thái mua thành công
     */
    @PostMapping("/buy-now/{itemId}")
    public ResponseEntity<BaseResponse> buyNow(
            @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
            @PathVariable Long itemId
    ) {
        BaseResponse response = auctionService.buyItemNow(
                itemId,
                userDetailsImpl.getUsername()
        );
        return ResponseEntity.ok().body(response);
    }

    /**
     * API thiết lập cấu hình tự động đặt giá (Auto-Bid) cho một sản phẩm đấu giá.
     * POST /auto-bid
     *
     * @param userDetailsImpl Thông tin người dùng muốn kích hoạt tự động đấu giá
     * @param request         Yêu cầu chứa mã sản phẩm và hạn mức giá cược tối đa
     * @return ResponseEntity phản hồi trạng thái thiết lập thành công
     */
    @PostMapping("/auto-bid")
    public ResponseEntity<BaseResponse> createAutoBid(
            @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
            @Valid @RequestBody AutoBidRequest request
    ) {
        BaseResponse response = auctionService.createAutoBid(
                request,
                userDetailsImpl.getUsername()
        );
        return ResponseEntity.ok().body(response);
    }
}
