package com.auction.users;

import com.auction.users.dto.AuthResponse;
import com.auction.users.dto.LoginRequest;
import com.auction.users.dto.RefreshTokenRequest;
import com.auction.users.dto.RegisterRequest;
import com.auction.users.dto.UserResponse;
import jakarta.validation.Valid;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auction.bids.Bid;
import com.auction.common.BaseObjectResponse;
import com.auction.common.jointdata.BidAndItem;
import com.auction.security.UserDetailsImpl;
import com.auction.users.dto.AuthResponse;
import com.auction.users.dto.BalanceResponse;
import com.auction.users.dto.DepositRequest;
import com.auction.users.dto.LoginRequest;
import com.auction.users.dto.RegisterRequest;
import com.auction.users.dto.UserResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse serviceResponse = userService.userRegister(request);
        return ResponseEntity.ok(serviceResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse serviceResponse = userService.userLogin(request);
        return ResponseEntity.ok(serviceResponse);
    }

    /**
     * Endpoint to refresh an access token.
     *
     * @param request The refresh token request.
     * @return A response entity containing the new access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse serviceResponse = userService.refreshToken(request);
        return ResponseEntity.ok(serviceResponse);
    }
    // API Endpoints for user
    @PostMapping("/me/deposit")
    public ResponseEntity<BalanceResponse> deposit(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
            @Valid @RequestBody DepositRequest request) {
        BalanceResponse response = userService.depositCredit(userDetailsImpl.getUsername(), request.getAmount());
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/me/balance")
    public ResponseEntity<BalanceResponse> balance(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
        BalanceResponse response = userService.getBalance(userDetailsImpl.getUsername());
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/me/bids")
    public ResponseEntity<BaseObjectResponse<Page<Bid>>> bids(
            @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
            @Min(0) @RequestParam(value = "0") int page,
            @Min(1) @Max(20) @RequestParam(value = "10") int size) {
        BaseObjectResponse<Page<Bid>> response = userService.getMyCurrentBids(userDetailsImpl.getUsername(), page,
                size);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/me/wins")
    public ResponseEntity<BaseObjectResponse<List<BidAndItem>>> getWins(
            @AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
        BaseObjectResponse<List<BidAndItem>> response = userService.getMyWinnings(userDetailsImpl.getUsername());
        return ResponseEntity.ok().body(response);
    }

}
