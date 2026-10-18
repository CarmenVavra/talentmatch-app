package de.talentmatch.auth;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    boolean existsByUserAndCreatedAtAfter(UserAccount user, LocalDateTime since);

    @Modifying
    @Query("delete from PasswordResetToken t where t.user = :user")
    void deleteByUser(@Param("user") UserAccount user);

    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
