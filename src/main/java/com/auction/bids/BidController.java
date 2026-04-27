package com.auction.bids;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.auction.bids.dto.*;
import com.auction.common.BaseResponse;
import com.auction.security.UserDetailsImpl;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/bids")
public class BidController {
    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping("")
    public ResponseEntity<BidPostResponse> makeBid(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
            @Valid @RequestBody BidPostRequest request) {

        BidPostResponse response = bidService.createBid(request, userDetailsImpl.getUsername());
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/buy-now/{itemId}")
    public ResponseEntity<BaseResponse> buyNow(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
            @PathVariable Long itemId) {
        BaseResponse response = bidService.buyItemNow(itemId, userDetailsImpl.getUsername());
        return ResponseEntity.ok().body(response);
    }
}