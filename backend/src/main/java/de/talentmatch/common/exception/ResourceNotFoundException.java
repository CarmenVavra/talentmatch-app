package de.talentmatch.common.exception;

/**
 * Eine angefragte Ressource existiert nicht (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " mit ID " + id + " wurde nicht gefunden.");
    }
}
