package de.talentmatch.talentpool.api;

import java.time.LocalDateTime;

import de.talentmatch.talentpool.TalentPoolStatus;

/**
 * Antwortseite ohne Anmeldung – bewusst nur das Nötigste (Vorname, Stelle, Fristen), keine Kontaktdaten.
 */
public record PublicTalentPoolDto(
        String firstName,
        String jobTitle,
        TalentPoolStatus status,
        LocalDateTime responseDeadline,
        LocalDateTime expiresAt) {
}
