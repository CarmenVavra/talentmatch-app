package de.talentmatch.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Aktuelles Passwort ist erforderlich.")
        String currentPassword,
        @NotBlank(message = "Neues Passwort ist erforderlich.")
        @Size(min = 8, max = 100, message = "Das neue Passwort muss mindestens 8 Zeichen lang sein.")
        String newPassword) {
}
