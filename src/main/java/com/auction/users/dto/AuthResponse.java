package com.auction.users.dto;

/**
 * Represents the response for authentication requests.
 *
 * @param success       Indicates whether the authentication was successful.
 * @param message       A message providing details about the authentication result.
 * @param token         The access token.
 * @param refreshToken  The refresh token.
 */
public record AuthResponse(
        boolean success,
        String message,
        String token,
        String refreshToken) {
}
