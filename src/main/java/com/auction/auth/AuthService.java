package com.auction.auth;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.auth.dto.AuthResponse;
import com.auction.auth.dto.LoginRequest;
import com.auction.auth.dto.RegisterRequest;
import com.auction.auth.jwtools.JwtUtil;
import com.auction.auth.jwtools.UserDetailsImpl;
import com.auction.common.BaseException;
import com.auction.common.BaseResponse;
import com.auction.users.User;
import com.auction.users.UserService;

/**
 * Service xử lý các nghiệp vụ đăng ký, đăng nhập, đăng xuất và gia hạn token của người dùng.
 */
@Service
public class AuthService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    // Thời gian sống của Refresh Token (được cấu hình trong file application.properties)
    @Value("${jwt.refreshExpiration}")
    private Long refreshLifetime;

    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    // Chuỗi băm nhận diện người dùng bị cấm (banned)
    @Value("${ban_hash}")
    private String banHash;

    public AuthService(RefreshTokenRepository refreshTokenRepository, JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder, UserService userService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    /**
     * Thực hiện nghiệp vụ làm mới (refresh) token khi Access Token hết hạn.
     *
     * @param refreshToken Chuỗi Refresh Token hiện tại của người dùng
     * @return AuthResponse chứa Access Token mới và Refresh Token mới
     */
    @Transactional
    public AuthResponse refreshingToken(String refreshToken) {
        // Tìm thông tin Refresh Token trong DB
        RefreshToken token = refreshTokenRepository.findRefreshTokenData(refreshToken)
                .orElseThrow(() -> new BaseException("invalid refresh token"));

        // Kiểm tra xem Refresh Token đã hết hạn chưa
        boolean isTokenExpired = token.getCreatedAt() + refreshLifetime < Instant.now().toEpochMilli();
        if (isTokenExpired) {
            throw new BaseException("Refresh token has expired, please login again");
        }

        // Tạo token mới và cập nhật lại vào DB
        String newRefreshToken = jwtUtil.generateRefreshToken(token.getUsername());
        token.setRefreshToken(newRefreshToken);
        String accessToken = jwtUtil.generateToken(token.getUsername());
        refreshTokenRepository.save(token);

        return new AuthResponse(true, "successfully refresh token", accessToken, newRefreshToken);
    }

    /**
     * Đăng ký tài khoản người dùng mới.
     *
     * @param request Yêu cầu đăng ký chứa username, displayName, password
     * @return BaseResponse phản hồi trạng thái
     */
    @Transactional
    public BaseResponse userRegister(RegisterRequest request) {
        // Kiểm tra tên tài khoản đã được đăng ký trước đó chưa
        if (userService.existsUsername(request.username())) {
            throw new BaseException("Username has already been taken");
        }

        // Mã hóa mật khẩu trước khi lưu vào cơ sở dữ liệu
        String hashedPassword = passwordEncoder.encode(request.password());

        // Tạo người dùng mới với số dư ví mặc định là 0.0
        User user = new User(request.username(), request.displayName(), hashedPassword, 0.0);
        userService.saveUser(user);
        return new BaseResponse(true, "Successfully registered.");
    }

    /**
     * Xác thực thông tin và đăng nhập tài khoản người dùng.
     *
     * @param request Yêu cầu đăng nhập chứa username và password
     * @return AuthResponse chứa thông tin đăng nhập thành công kèm Access Token và Refresh Token
     */
    @Transactional
    public AuthResponse loginUser(LoginRequest request) {
        User user = userService.getUserByUsername(request.username());

        // Kiểm tra xem người dùng có bị cấm (banned) hay không
        if (user.getHashedPassword().equals(banHash)) {
            throw new BaseException("User was banned");
        }

        // Kiểm tra tính chính xác của mật khẩu
        if (!passwordEncoder.matches(request.password(), user.getHashedPassword())) {
            throw new BaseException("Invalid username or password");
        }

        // Tạo mã Access Token và Refresh Token mới
        String accessToken = jwtUtil.generateToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // Lưu thông tin Refresh Token vào cơ sở dữ liệu để phục vụ kiểm tra và gia hạn sau này
        refreshTokenRepository.save(new RefreshToken(user.getUsername(), refreshToken));

        return new AuthResponse(true, "Successfully logged in.", accessToken, refreshToken);
    }

    /**
     * Thu hồi/xóa bỏ Refresh Token của người dùng khỏi DB khi đăng xuất hoặc tài khoản bị vô hiệu hóa.
     *
     * @param username Tên đăng nhập người dùng cần thu hồi
     */
    @Transactional
    public void revokeToken(String username) {
        refreshTokenRepository.deleteById(username);
    }

    /**
     * Thực hiện đăng xuất tài khoản và thu hồi token hiện tại.
     *
     * @param userDetailsImpl Thông tin người dùng hiện tại đang đăng nhập
     * @return BaseResponse phản hồi trạng thái đăng xuất
     */
    @Transactional
    public BaseResponse logoutUser(UserDetailsImpl userDetailsImpl) {
        revokeToken(userDetailsImpl.getUsername());
        return new BaseResponse(true, "successfully logout");
    }
}
