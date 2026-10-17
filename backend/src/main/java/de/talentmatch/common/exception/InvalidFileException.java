package de.talentmatch.common.exception;

/**
 * Eine hochgeladene Datei ist ungültig (falscher Typ, zu groß, leer, nicht lesbar) – HTTP 400.
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }

    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
