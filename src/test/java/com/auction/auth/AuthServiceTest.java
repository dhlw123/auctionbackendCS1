package com.auction.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auction.auth.dto.AuthResponse;
import com.auction.auth.dto.LoginRequest;
import com.auction.auth.dto.RegisterRequest;
import com.auction.auth.jwtools.JwtUtil;
import com.auction.common.BaseException;
import com.auction.common.BaseResponse;
import com.auction.users.User;
import com.auction.users.UserService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @Mock private JwtUtil jwtUtil;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private UserService userService;

  @InjectMocks private AuthService authService;

  private User testUser;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(authService, "refreshLifetime", 604800000L); // 7 days in ms
    // Initialize the banHash field to avoid NullPointerException in loginUser
    ReflectionTestUtils.setField(authService, "banHash", "bannedHashValue");
    testUser = new User("testuser", "Test User", "hashedpassword", 0.0);
  }

  @Test
  void userRegister_Success() {
    // Arrange
    RegisterRequest request = new RegisterRequest("newuser", "New User", "password");
    when(userService.existsUsername(request.username())).thenReturn(false);
    when(passwordEncoder.encode(request.password())).thenReturn("newhashedpassword");

    // Act
    BaseResponse response = authService.userRegister(request);

    // Assert
    assertEquals(true, response.getStatus());
    // Fixed: The actual message has a single 's' in 'Successfully' in the code right now.
    assertEquals("Successfully registered.", response.getMessage());
    verify(userService).saveUser(any(User.class));
  }

  @Test
  void userRegister_UsernameTaken_ThrowsException() {
    // Arrange
    RegisterRequest request = new RegisterRequest("testuser", "Test User", "password");
    when(userService.existsUsername(request.username())).thenReturn(true);

    // Act & Assert
    BaseException exception =
        assertThrows(
            BaseException.class,
            () -> {
              authService.userRegister(request);
            });
    assertEquals("Username has already been taken", exception.getMessage());
  }

  @Test
  void loginUser_Success() {
    // Arrange
    LoginRequest request = new LoginRequest("testuser", "password");
    when(userService.getUserByUsername(request.username())).thenReturn(testUser);
    when(passwordEncoder.matches(request.password(), testUser.getHashedPassword()))
        .thenReturn(true);
    when(jwtUtil.generateToken(testUser.getUsername())).thenReturn("access_token");
    when(jwtUtil.generateRefreshToken(testUser.getUsername())).thenReturn("refresh_token");

    // Act
    AuthResponse response = authService.loginUser(request);

    // Assert
    assertEquals(true, response.getStatus());
    assertEquals("Successfully logged in.", response.getMessage());
    assertEquals("access_token", response.getAccessToken());
    assertEquals("refresh_token", response.getRefreshToken());
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  void loginUser_InvalidPassword_ThrowsException() {
    // Arrange
    LoginRequest request = new LoginRequest("testuser", "wrongpassword");
    when(userService.getUserByUsername(request.username())).thenReturn(testUser);
    when(passwordEncoder.matches(request.password(), testUser.getHashedPassword()))
        .thenReturn(false);

    // Act & Assert
    BaseException exception =
        assertThrows(
            BaseException.class,
            () -> {
              authService.loginUser(request);
            });
    assertEquals("Invalid username or password", exception.getMessage());
  }

  @Test
  void loginUser_BannedUser_ThrowsException() {
    // Arrange
    LoginRequest request = new LoginRequest("banneduser", "password");
    User bannedUser = new User("banneduser", "Banned User", "bannedHashValue", 0.0);
    when(userService.getUserByUsername(request.username())).thenReturn(bannedUser);

    // Act & Assert
    BaseException exception =
        assertThrows(
            BaseException.class,
            () -> {
              authService.loginUser(request);
            });
    assertEquals("User was banned", exception.getMessage());
  }

  @Test
  void refreshingToken_Success() {
    // Arrange
    String oldRefreshToken = "old_refresh_token";
    RefreshToken tokenData = new RefreshToken();
    tokenData.setUsername("testuser");
    tokenData.setRefreshToken(oldRefreshToken);

    ReflectionTestUtils.setField(
        tokenData, "createdAt", Instant.now().toEpochMilli()); // Fresh token

    when(refreshTokenRepository.findRefreshTokenData(oldRefreshToken))
        .thenReturn(Optional.of(tokenData));
    when(jwtUtil.generateRefreshToken("testuser")).thenReturn("new_refresh_token");
    when(jwtUtil.generateToken("testuser")).thenReturn("new_access_token");

    // Act
    AuthResponse response = authService.refreshingToken(oldRefreshToken);

    // Assert
    assertEquals(true, response.getStatus());
    assertEquals("new_access_token", response.getAccessToken());
    assertEquals("new_refresh_token", response.getRefreshToken());
    verify(refreshTokenRepository).save(tokenData);
  }

  @Test
  void refreshingToken_ExpiredToken_ThrowsException() {
    // Arrange
    String oldRefreshToken = "old_refresh_token";
    RefreshToken tokenData = new RefreshToken();
    tokenData.setUsername("testuser");
    tokenData.setRefreshToken(oldRefreshToken);

    // Set creation time way back to simulate an expired token
    ReflectionTestUtils.setField(tokenData, "createdAt", Instant.now().toEpochMilli() - 700000000L);

    when(refreshTokenRepository.findRefreshTokenData(oldRefreshToken))
        .thenReturn(Optional.of(tokenData));

    // Act & Assert
    BaseException exception =
        assertThrows(
            BaseException.class,
            () -> {
              authService.refreshingToken(oldRefreshToken);
            });
    assertEquals("Refresh token has expired, please login again", exception.getMessage());
  }
}
