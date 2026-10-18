package de.talentmatch.common.exception;

/**
 * Ein Link mit geheimem Token ist ungültig, abgelaufen oder bereits verwendet – HTTP 400.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
