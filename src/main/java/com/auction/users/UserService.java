package com.auction.users;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.auction.bids.Bid;
import com.auction.bids.BidRepository;
import com.auction.common.BaseObjectResponse;
import com.auction.common.jointdata.BidAndItem;
import com.auction.security.JwtUtil;
import com.auction.users.dto.AuthResponse;
import com.auction.users.dto.BalanceResponse;
import com.auction.users.dto.LoginRequest;
import com.auction.users.dto.RegisterRequest;
import com.auction.users.dto.UserResponse;
import com.auction.users.exceptions.UserException;

import jakarta.transaction.Transactional;;;

@Service
public class UserService {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BidRepository bidRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
            BidRepository bidRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.bidRepository = bidRepository;
    }

    @Transactional
    public UserResponse userRegister(RegisterRequest request) {
        String hashedPassword = passwordEncoder.encode(request.password());
        if (userRepository.existsByUsername(request.username())) {
            throw new UserException(false, "Username has already been taken");
        }
        User user = new User(request.username(), request.displayName(), hashedPassword, 0.0);
        user = userRepository.save(user);
        return user.toResponse();
    }

    @Transactional
    public AuthResponse userLogin(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UserException(false, "Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getHashedPassword())) {
            throw new UserException(false, "Invalid username or password");
        }

        return new AuthResponse(true, "Login successful", jwtUtil.generateToken(user.getUsername()));
    }

    @Transactional
    public User getUserReferenceByUsername(String username) {
        User userRef = userRepository.getReferenceById(username);
        return userRef;
    }

    @Transactional
    public User getUserByUsername(String username) {
        User request = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException(false, "Invalid username"));
        return request;
    }

    @Transactional
    public BalanceResponse depositCredit(String username, Double creditAmount) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException(false, "Invalid username"));
        user.setBalance(user.getBalance() + creditAmount);
        user = userRepository.save(user);
        return new BalanceResponse(true,
                "Succesfully deposited credit, current balance",
                user.getBalance());

    }

    @Transactional
    public BalanceResponse getBalance(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException(false, "Invalid username"));
        return new BalanceResponse(true, "Get balance successful", user.getBalance());
    }

    @Transactional
    public BaseObjectResponse<Page<Bid>> getMyCurrentBids(String username, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        User userRef = userRepository.getReferenceById(username);

        Page<Bid> bids = bidRepository.findAllByUser(userRef, pageable);

        return new BaseObjectResponse<Page<Bid>>(true, "succesfully got my bids", bids);
    }

    @Transactional
    public BaseObjectResponse<List<BidAndItem>> getMyWinnings(String username) {

        List<Bid> bids = bidRepository.getWinsByUser(username, Instant.now().getEpochSecond());
        ArrayList<BidAndItem> items = new ArrayList<BidAndItem>();
        for (Bid bid : bids) {
            items.add(new BidAndItem(bid, bid.getItem()));
        }
        return new BaseObjectResponse<List<BidAndItem>>(true, "sucesfully returned winnings", items);

    }
}
