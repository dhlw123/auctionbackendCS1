package com.auction.users;

import com.auction.users.dto.AuthResponse;
import com.auction.users.dto.LoginRequest;
import com.auction.users.dto.RefreshTokenRequest;
import com.auction.users.dto.RegisterRequest;
import com.auction.users.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
