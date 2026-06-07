package com.auction.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository thao tác cơ sở dữ liệu với thực thể RevokedToken. */
@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {}
