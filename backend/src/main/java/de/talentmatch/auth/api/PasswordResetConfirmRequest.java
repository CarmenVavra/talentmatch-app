package de.talentmatch.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Neues Passwort über den Einmal-Link aus der E-Mail setzen.
 */
public record PasswordResetConfirmRequest(
        @NotBlank(message = "Der Link ist unvollständig.")
        String token,
        @NotBlank(message = "Passwort ist erforderlich.")
        @Size(min = 8, max = 100, message = "Das Passwort muss mindestens 8 Zeichen lang sein.")
        String newPassword) {
}
