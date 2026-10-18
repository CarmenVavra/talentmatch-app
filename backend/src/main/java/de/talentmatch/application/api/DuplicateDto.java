package de.talentmatch.application.api;

import java.time.LocalDateTime;
import java.util.List;

import de.talentmatch.application.ApplicationStatus;
import de.talentmatch.resume.DocumentType;
import de.talentmatch.resume.api.GroupMatch;

/**
 * Mögliches Duplikat einer Bewerbung (gleiche Stelle, gleiche E-Mail, Telefonnummer oder gleicher Name).
 */
public record DuplicateDto(
        Long id,
        String firstName,
        String lastName,
        LocalDateTime createdAt,
        ApplicationStatus status,
        GroupMatch reason,
        List<DocumentRefDto> documents) {

    /**
     * Unterlage des Duplikats; {@code suggestedAs} schlägt vor, als was sie übernommen werden soll.
     */
    public record DocumentRefDto(DocumentType type, String fileName, DocumentType suggestedAs) {
    }
}
