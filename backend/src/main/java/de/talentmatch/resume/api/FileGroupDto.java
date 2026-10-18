package de.talentmatch.resume.api;

import java.util.List;

/**
 * Vorschlag: Diese Dateien gehören zu einer Person und ergeben eine Bewerbung.
 * {@code needsReview} ist gesetzt, wenn die Zuordnung nur auf dem Namen beruht oder Dokumentarten doppelt sind.
 */
public record FileGroupDto(List<Integer> fileIndexes, GroupMatch match, boolean needsReview) {
}
