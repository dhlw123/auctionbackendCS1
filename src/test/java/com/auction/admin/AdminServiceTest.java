package com.auction.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.auction.admin.dto.UnbanRequest;
import com.auction.auth.AuthService;
import com.auction.auth.RevokedToken;
import com.auction.auth.RevokedTokenRepository;
import com.auction.common.BaseException;
import com.auction.common.BaseResponse;
import com.auction.items.Item;
import com.auction.items.ItemService;
import com.auction.users.User;
import com.auction.users.UserService;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ItemService itemService;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @InjectMocks
    private AdminService adminService;

    private User normalUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminService, "banHash", "BANNED_HASH");
        normalUser = new User("baduser", "Bad User", "hashedpw", 1000.0);
        adminUser = new User("admin", "Admin", "hashedpw", 0.0);
    }

    @Test
    @DisplayName("Khóa tài khoản người dùng thành công")
    void banUser_Success() {
        // Arrange
        when(userService.getUserByUsername("baduser")).thenReturn(normalUser);

        // Act
        BaseResponse response = adminService.banUser("baduser");

        // Assert
        assertTrue(response.getStatus());
        assertEquals("successfully banned user", response.getMessage());
        assertEquals("BANNED_HASH", normalUser.getHashedPassword());
        verify(authService, times(2)).revokeToken("baduser");
        verify(userService).saveUser(normalUser);
        verify(revokedTokenRepository).save(any(RevokedToken.class));
    }

    @Test
    @DisplayName("Không cho phép khóa tài khoản admin")
    void banUser_AdminAccount_ThrowsException() {
        // Act & Assert
        BaseException ex = assertThrows(BaseException.class,
                () -> adminService.banUser("admin"));
        assertEquals("You can't ban admin", ex.getMessage());
    }

    @Test
    @DisplayName("Mở khóa tài khoản người dùng thành công")
    void unbanUser_Success() {
        // Arrange
        normalUser.setHashedPassword("BANNED_HASH"); // Đã bị ban
        UnbanRequest request = new UnbanRequest("baduser", "newpassword123");
        when(userService.getUserByUsername("baduser")).thenReturn(normalUser);
        when(passwordEncoder.encode("newpassword123")).thenReturn("new_hashed_pw");

        // Act
        BaseResponse response = adminService.unbanUser(request);

        // Assert
        assertTrue(response.getStatus());
        assertEquals("Succesfully unbanned user.", response.getMessage());
        assertEquals("new_hashed_pw", normalUser.getHashedPassword());
        verify(revokedTokenRepository).deleteById("baduser");
    }

    @Test
    @DisplayName("Hủy phiên đấu giá bởi admin thành công")
    void cancelAuction_Success() {
        // Arrange
        Long itemId = 1L;
        User seller = new User("seller", "Seller", "hashedpw", 0.0);
        Item item = new Item(seller, "Test", "Desc");
        BaseResponse cancelResponse = new BaseResponse(true, "Item successfully canceled.");

        when(itemService.getItem(itemId)).thenReturn(item);
        when(itemService.cancelItem(itemId, "seller")).thenReturn(cancelResponse);

        // Act
        BaseResponse response = adminService.cancelAuction(itemId);

        // Assert
        assertTrue(response.getStatus());
        assertEquals("Item successfully canceled.", response.getMessage());
    }
}
