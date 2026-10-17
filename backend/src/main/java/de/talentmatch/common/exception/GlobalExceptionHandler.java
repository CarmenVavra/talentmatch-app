package de.talentmatch.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Zentrale Fehlerbehandlung: alle API-Fehler als RFC-9457 {@link ProblemDetail}, ohne Stacktraces.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Nicht gefunden", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Nicht gefunden", "Der angefragte Endpunkt existiert nicht.");
    }

    @ExceptionHandler(InvalidFileException.class)
    public ProblemDetail handleInvalidFile(InvalidFileException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Ungültige Datei", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Ungültige Datei", "Die Datei ist größer als 10 MB.");
    }

    @ExceptionHandler({MultipartException.class, MissingServletRequestPartException.class})
    public ProblemDetail handleMultipart(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "Ungültige Datei", "Es wurde keine gültige Datei übermittelt.");
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ProblemDetail handleInvalidPassword(InvalidPasswordException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Passwort ungültig", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        return problem(HttpStatus.CONFLICT, "Aktion nicht möglich", ex.getMessage());
    }

    @ExceptionHandler(AiAnalysisException.class)
    public ProblemDetail handleAi(AiAnalysisException ex) {
        LOG.warn("KI-Analyse fehlgeschlagen: {}", ex.getMessage(), ex.getCause());
        return problem(HttpStatus.BAD_GATEWAY, "KI-Analyse fehlgeschlagen", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Anmeldung fehlgeschlagen", "E-Mail oder Passwort ist falsch.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Kein Zugriff", "Für diese Aktion fehlt die Berechtigung.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validierungsfehler",
                "Bitte die markierten Eingaben prüfen.");
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail handleUnreadable(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "Ungültige Anfrage", "Die Anfrage konnte nicht gelesen werden.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        LOG.error("Unerwarteter Fehler", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Interner Fehler", "Ein unerwarteter Fehler ist aufgetreten.");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
