package de.talentmatch.auth;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Einmal-Link zum Zurücksetzen des Passworts. Gespeichert wird nur der SHA-256-Hash des Tokens, nie das Token
 * selbst.
 */
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(UserAccount user, String tokenHash, LocalDateTime createdAt,
                              LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isValidAt(LocalDateTime now) {
        return now.isBefore(expiresAt);
    }

    public UserAccount getUser() {
        return user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
