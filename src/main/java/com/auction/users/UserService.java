package com.auction.users;

import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.auction.users.dto.*;

import jakarta.transaction.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @Transactional
    public UserResponse userSignin(RegisterRequest request) {
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Username has already been taken"); 
        }
        User user = new User(request.getUsername(), request.getDisplayName(), hashedPassword, 0.0);
        user = userRepository.save(user);
        return user.toResponse();
    }
}
