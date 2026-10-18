package de.talentmatch.resume.api;

import de.talentmatch.resume.DocumentType;

/**
 * Vorab ausgewertete Datei eines HR-Uploads ({@code error} gesetzt, wenn sie nicht verwendbar ist).
 */
public record InspectedFileDto(
        int index,
        String fileName,
        DocumentType type,
        String firstName,
        String lastName,
        String email,
        String phone,
        String error) {
}
