package de.talentmatch.common.exception;

/**
 * Das aktuelle Passwort ist falsch oder das neue Passwort ist nicht zulässig – HTTP 400.
 */
public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException(String message) {
        super(message);
    }
}
