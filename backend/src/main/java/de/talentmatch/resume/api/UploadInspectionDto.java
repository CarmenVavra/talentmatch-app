package de.talentmatch.resume.api;

import java.util.List;

/**
 * Ergebnis der Vorabprüfung eines HR-Uploads: Angaben je Datei und vorgeschlagene Gruppen je Person.
 */
public record UploadInspectionDto(List<InspectedFileDto> files, List<FileGroupDto> groups) {
}
