package com.himanshu.social_post_backend.repository;

import com.himanshu.social_post_backend.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUsername(String username);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.username = :username")
    void revokeAllByUsername(String username);

    void deleteByExpiryDateBefore(Instant threshold);
}
