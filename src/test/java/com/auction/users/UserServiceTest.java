package com.auction.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBalanceSink userBalanceSink;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "Test User", "hashedpw", 5000.0);
    }

    @Test
    @DisplayName("Nạp tiền thành công - Số dư tăng đúng")
    void addBalance_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        Double newBalance = userService.addBalance("testuser", 1000.0);

        // Assert
        assertEquals(6000.0, newBalance);
        verify(userRepository).save(testUser);
        verify(userBalanceSink).pushNewBalance("testuser", 6000.0);
    }

    @Test
    @DisplayName("Trừ tiền thành công - Số dư giảm đúng")
    void deductBalance_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        userService.deductBalance("testuser", 1000.0);

        // Assert
        assertEquals(4000.0, testUser.getBalance());
        verify(userRepository).save(testUser);
        verify(userBalanceSink).pushNewBalance("testuser", 4000.0);
    }

    @Test
    @DisplayName("Nạp tiền vào tài khoản - depositCredit trả về BaseObjectResponse đúng")
    void depositCredit_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        BaseObjectResponse<Double> response = userService.depositCredit("testuser", 2000.0);

        // Assert
        assertTrue(response.getStatus());
        assertEquals(7000.0, response.getEntity());
    }

    @Test
    @DisplayName("Lấy số dư thành công")
    void getBalance_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        BaseObjectResponse<Double> response = userService.getBalance("testuser");

        // Assert
        assertTrue(response.getStatus());
        assertEquals(5000.0, response.getEntity());
    }

    @Test
    @DisplayName("Lấy số dư thất bại - Người dùng không tồn tại")
    void getBalance_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // Act & Assert
        BaseException ex = assertThrows(BaseException.class,
                () -> userService.getBalance("ghost"));
        assertEquals("Invalid username", ex.getMessage());
    }

    @Test
    @DisplayName("Tìm người dùng thất bại - Ném lỗi khi không tồn tại")
    void getUserByUsername_NotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // Act & Assert
        BaseException ex = assertThrows(BaseException.class,
                () -> userService.getUserByUsername("ghost"));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    @DisplayName("Kiểm tra username đã tồn tại")
    void existsUsername_ReturnsTrue() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        assertTrue(userService.existsUsername("testuser"));
    }

    @Test
    @DisplayName("Lưu user thành công")
    void saveUser_Success() {
        // Arrange
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        User saved = userService.saveUser(testUser);

        // Assert
        assertEquals("testuser", saved.getUsername());
        verify(userRepository).save(testUser);
    }
}
