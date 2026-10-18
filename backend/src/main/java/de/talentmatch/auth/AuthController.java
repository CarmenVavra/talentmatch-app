package de.talentmatch.auth;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.talentmatch.auth.api.ChangePasswordRequest;
import de.talentmatch.auth.api.LoginRequest;
import de.talentmatch.auth.api.LoginResponse;
import de.talentmatch.auth.api.PasswordResetConfirmRequest;
import de.talentmatch.auth.api.PasswordResetRequest;
import de.talentmatch.auth.api.RegisterRequest;
import de.talentmatch.auth.api.UserDto;
import de.talentmatch.common.exception.ResourceNotFoundException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return authService.findUser(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("Benutzer", jwt.getSubject()));
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(jwt.getSubject(), request.currentPassword(), request.newPassword());
    }

    /**
     * Antwortet immer mit 204 – auch wenn es zur Adresse kein Konto gibt.
     */
    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestReset(request.email());
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
    }
}
