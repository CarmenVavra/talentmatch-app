package de.talentmatch.auth;

import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.talentmatch.auth.api.LoginResponse;
import de.talentmatch.auth.api.UserDto;
import de.talentmatch.common.exception.InvalidPasswordException;
import de.talentmatch.common.exception.ResourceNotFoundException;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserAccountRepository userRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String password) {
        UserAccount user = userRepository.findByEmailIgnoreCase(email.trim())
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Ungültige Zugangsdaten"));
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
