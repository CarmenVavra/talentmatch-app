package de.talentmatch.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "E-Mail ist erforderlich.")
        @Email(message = "Bitte eine gültige E-Mail-Adresse eingeben.")
        String email,
        @NotBlank(message = "Passwort ist erforderlich.")
        String password) {
}
