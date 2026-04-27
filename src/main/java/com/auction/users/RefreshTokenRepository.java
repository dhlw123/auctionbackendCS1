package com.auction.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
* Repository interface for managing {@link RefreshToken} entities.
**/
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * Finds a refresh token by its token string.
     *
     * @param refreshToken The refresh token string to search for.
     * @return An {@link Optional} containing the found refresh token, or empty if not found.
     */
    Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
