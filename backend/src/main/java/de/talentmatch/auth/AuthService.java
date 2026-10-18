package de.talentmatch.auth;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.talentmatch.auth.api.LoginResponse;
import de.talentmatch.auth.api.RegisterRequest;
import de.talentmatch.auth.api.UserDto;
import de.talentmatch.common.exception.BusinessRuleException;
import de.talentmatch.common.exception.InvalidPasswordException;
import de.talentmatch.common.exception.ResourceNotFoundException;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttempts;
    private final Clock clock;

    public AuthService(UserAccountRepository userRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService, LoginAttemptService loginAttempts, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginAttempts = loginAttempts;
        this.clock = clock;
    }

    /**
     * Meldet an. Nach zu vielen Fehlversuchen ist die Anmeldung für die Adresse vorübergehend gesperrt
     * (auch für unbekannte Adressen, damit sich nicht erkennen lässt, ob es ein Konto gibt).
     */
    @Transactional(readOnly = true)
    public LoginResponse login(String email, String password) {
        loginAttempts.checkNotLocked(email);
        UserAccount user = userRepository.findByEmailIgnoreCase(email.trim())
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElseThrow(() -> {
                    loginAttempts.recordFailure(email);
                    return new BadCredentialsException("Ungültige Zugangsdaten");
                });
        loginAttempts.reset(email);
        TokenService.IssuedToken token = tokenService.issue(user);
        return new LoginResponse(token.value(), token.expiresAt(), UserDto.from(user));
    }

    /**
     * Legt ein Bewerber-Konto an (immer Rolle APPLICANT), vermerkt die Bestätigung der Datenschutzhinweise und
     * meldet es direkt an.
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new BusinessRuleException("Für diese E-Mail-Adresse existiert bereits ein Konto.");
        }
        UserAccount account = new UserAccount(email, passwordEncoder.encode(request.password()),
                request.firstName().trim(), request.lastName().trim(), null, Role.APPLICANT);
        account.acceptPrivacy(LocalDateTime.now(clock));
        UserAccount user = userRepository.save(account);
        TokenService.IssuedToken token = tokenService.issue(user);
        return new LoginResponse(token.value(), token.expiresAt(), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email).map(UserDto::from);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Benutzer", email));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidPasswordException("Das aktuelle Passwort ist nicht korrekt.");
        }
        if (currentPassword.equals(newPassword)) {
            throw new InvalidPasswordException("Das neue Passwort muss sich vom aktuellen unterscheiden.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }
}
