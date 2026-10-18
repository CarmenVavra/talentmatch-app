package de.talentmatch.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import de.talentmatch.auth.api.LoginResponse;
import de.talentmatch.auth.api.RegisterRequest;
import de.talentmatch.common.exception.BusinessRuleException;
import de.talentmatch.common.exception.InvalidPasswordException;

class AuthServiceTest {

    private static final String EMAIL = "hr@example.org";

    private final PasswordEncoder encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    private UserAccountRepository repository;
    private AuthService service;
    private UserAccount user;

    @BeforeEach
    void setUp() {
        repository = mock(UserAccountRepository.class);
        TokenService tokenService = mock(TokenService.class);
        when(tokenService.issue(any())).thenReturn(new TokenService.IssuedToken("jwt", Instant.EPOCH));
        service = new AuthService(repository, encoder, tokenService, mock(LoginAttemptService.class),
                Clock.fixed(Instant.parse("2026-10-18T08:00:00Z"), ZoneOffset.UTC));
        user = new UserAccount(EMAIL, encoder.encode("alt-passwort"), "Katrin", "Vogel", "People & Culture",
                Role.HR_MANAGER);
        when(repository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
    }

    @Test
    void loginReturnsTokenAndUser() {
        LoginResponse response = service.login(" " + EMAIL + " ", "alt-passwort");

        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.user().role()).isEqualTo(Role.HR_MANAGER);
    }

    @Test
    void loginRejectsWrongPasswordAndUnknownUser() {
        assertThatThrownBy(() -> service.login(EMAIL, "falsch")).isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> service.login("x@example.org", "alt-passwort"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void registerCreatesApplicantAccountAndLogsIn() {
        List<UserAccount> saved = new ArrayList<>();
        when(repository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            saved.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });

        LoginResponse response = service.register(
                new RegisterRequest(" Max ", "Mustermann", " Max@Example.org ", "geheim-123", true));

        assertThat(saved.getFirst().getPrivacyAcceptedAt()).contains(LocalDateTime.of(2026, 10, 18, 8, 0));
        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.user().role()).isEqualTo(Role.APPLICANT);
        assertThat(response.user().email()).isEqualTo("max@example.org");
        assertThat(response.user().firstName()).isEqualTo("Max");
    }

    @Test
    void registerRejectsExistingEmail() {
        assertThatThrownBy(() -> service.register(new RegisterRequest("K", "V", EMAIL, "geheim-123", true)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void changePasswordStoresNewHash() {
        service.changePassword(EMAIL, "alt-passwort", "neues-passwort");

        assertThat(encoder.matches("neues-passwort", user.getPasswordHash())).isTrue();
    }

    @Test
    void changePasswordRequiresCorrectCurrentPassword() {
        assertThatThrownBy(() -> service.changePassword(EMAIL, "falsch", "neues-passwort"))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void changePasswordRejectsUnchangedPassword() {
        assertThatThrownBy(() -> service.changePassword(EMAIL, "alt-passwort", "alt-passwort"))
                .isInstanceOf(InvalidPasswordException.class);
    }
}
