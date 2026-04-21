package com.auction.users.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "username can't be blank") String username,
        @NotBlank(message = "display name can't be blank") String displayName,
        @NotBlank(message = "password can't be blank") String password) {
};

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
