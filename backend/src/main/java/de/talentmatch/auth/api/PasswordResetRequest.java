package de.talentmatch.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * „Passwort vergessen“: Link an diese Adresse schicken.
 */
public record PasswordResetRequest(
        @NotBlank(message = "E-Mail ist erforderlich.")
        @Email(message = "Bitte eine gültige E-Mail-Adresse eingeben.")
        String email) {
}
