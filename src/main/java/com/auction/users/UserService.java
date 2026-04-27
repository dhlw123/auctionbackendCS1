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
import com.auction.users.dto.RefreshTokenRequest;
import com.auction.users.dto.RegisterRequest;
import com.auction.users.dto.UserResponse;
import com.auction.users.exceptions.UserException;

import jakarta.transaction.Transactional;;;

@Service
public class UserService {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
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

    /**
     * Handles user login, generating both an access token and a refresh token.
     *
     * @param request The login request containing the user's credentials.
     * @return An {@link AuthResponse} containing the access token and refresh token.
     */
    @Transactional
    public AuthResponse userLogin(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UserException(false, "Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getHashedPassword())) {
            throw new UserException(false, "Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // Save the refresh token to the database
        refreshTokenRepository.save(new RefreshToken(user.getUsername(), refreshToken));

        return new AuthResponse(true, "Login successful", token, refreshToken);
    }

    /**
     * Refreshes an access token using a refresh token.
     *
     * @param request The refresh token request.
     * @return An {@link AuthResponse} containing the new access token and the original refresh token.
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        return refreshTokenRepository.findByRefreshToken(refreshToken)
                .map(token -> {
                    String username = token.getUsername();
                    String newAccessToken = jwtUtil.generateToken(username);
                    return new AuthResponse(true, "New access token generated", newAccessToken, refreshToken);
                })
                .orElseThrow(() -> new UserException(false, "Invalid refresh token"));
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
