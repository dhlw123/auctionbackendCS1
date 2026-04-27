package com.auction.users.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Represents a request to refresh an access token.
 *
 * @param refreshToken The refresh token.
 */
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}
