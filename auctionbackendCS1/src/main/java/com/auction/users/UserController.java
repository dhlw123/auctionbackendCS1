package com.auction.users;

import org.springframework.web.bind.annotation.RestController;
import com.auction.users.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signin")
    public ResponseEntity<UserResponse> signIn(@Valid @RequestBody RegisterRequest request) {
        UserResponse serviceResponse = userService.userSignin(request);
        return ResponseEntity.ok(serviceResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse serviceResponse = userService.userLogin(request);
        return ResponseEntity.ok(serviceResponse);
    }

}
