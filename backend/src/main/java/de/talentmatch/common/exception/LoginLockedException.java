package de.talentmatch.common.exception;

/**
 * Zu viele fehlgeschlagene Anmeldeversuche – die Anmeldung ist vorübergehend gesperrt (HTTP 429).
 */
public class LoginLockedException extends RuntimeException {

    public LoginLockedException(String message) {
        super(message);
    }
}
