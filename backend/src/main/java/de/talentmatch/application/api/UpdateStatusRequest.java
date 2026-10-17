package de.talentmatch.application.api;

import jakarta.validation.constraints.NotNull;

import de.talentmatch.application.ApplicationStatus;

public record UpdateStatusRequest(@NotNull(message = "Status ist erforderlich.") ApplicationStatus status) {
}
