package com.auction.users.dto;

import com.auction.common.annotations.NoSpace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotEmpty(message="Username must not be empty")
        @NotNull(message="Username must not be null")
        @NoSpace(message = "Username can't have space")
        String username,
        @NotNull(message = "Display name must not be null")
        @NotBlank(message = "Display name can't be blank")
        String displayName,
        @NotEmpty(message="Password must not be empty")
        @NotNull(message="Password must not be null")
        @NoSpace(message = "Password can't have space") String password) {
}

// @NotBlank(message = "display name can't be blank")
// private String displayName;
// @NotBlank(message = "password can't be blank")
// private String password;
// public String getUsername() {
// return username;
// }
// public String getDisplayName() {
// return displayName;
// }
// public String getPassword() {
// return password;
// }
// public void updateDisplayName(String name) {
// displayName = name;
// }

// public void setUsername(String username) {
// this.username = username;
// }
// public void setDisplayName(String displayname) {
// this.displayName = displayname;
// }
// public void setPassword(String password) {
// this.password = password;
// }
// }
