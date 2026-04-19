package com.auction.users;

import org.springframework.web.bind.annotation.RestController;

import com.auction.users.dto.RegisterRequest;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/signin")
    String signIn(@Valid @RequestBody RegisterRequest request) {
        // ResponseEntity<String> serviceResponse = 
        return "User Created";
    }
    
}
