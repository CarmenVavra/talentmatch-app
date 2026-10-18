package de.talentmatch.resume;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Einfache Vorbelegung von Stammdaten direkt nach dem Upload – bevor das LLM den Lebenslauf strukturiert.
 */
public final class ResumeHeuristics {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern PHONE = Pattern.compile("(\\+|00)\\d{2}[\\d ()/-]{6,18}\\d");
    /** Wörter im Dateinamen, die keine Namensbestandteile sind (Dokumentarten, Versionen). */
    private static final Pattern NOISE_WORDS = Pattern.compile("(?i)\\b(lebenslauf|cv|resume|vita|bewerbung"
            + "|bewerbungsschreiben|anschreiben|motivationsschreiben|cover|letter|skillprofil|skills?|profile?"
            + "|kompetenzprofil|kompetenzen|qualifikationen|final|neu|und|and)\\b");

    /** Platzhalter-Nachname, wenn der Dateiname keinen Namen enthält. */
    public static final String UNKNOWN_LAST_NAME = "Bewerber:in";

    private ResumeHeuristics() {
    }

    /**
     * Leitet Vor- und Nachname aus dem Dateinamen ab, z. B. {@code Lebenslauf_Maria_Schulz.pdf} → Maria / Schulz.
     */
    public static Name guessName(String fileName) {
        String base = fileName == null ? "" : fileName.replaceAll("(?i)\\.pdf$", "");
        base = NOISE_WORDS.matcher(base.replaceAll("[_\\-.]+", " ")).replaceAll(" ").trim();
        String[] parts = Arrays.stream(base.split("\\s+"))
                .filter(part -> !part.isBlank() && part.chars().noneMatch(Character::isDigit))
                .toArray(String[]::new);
        if (parts.length == 0) {
            return new Name("Neu", UNKNOWN_LAST_NAME);
        }
        String first = capitalize(parts[0]);
        String last = parts.length > 1
                ? String.join(" ", Arrays.stream(parts, 1, parts.length).map(ResumeHeuristics::capitalize).toList())
                : UNKNOWN_LAST_NAME;
        return new Name(first, last);
    }

    public static Optional<String> findEmail(String text) {
        Matcher matcher = EMAIL.matcher(text);
        return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
    }

    public static Optional<String> findPhone(String text) {
        Matcher matcher = PHONE.matcher(text);
        return matcher.find() ? Optional.of(matcher.group().trim()) : Optional.empty();
    }

    /**
     * Telefonnummer zum Vergleichen: nur die letzten neun Ziffern, damit „+43 660 …“ und „0660 …“ gleich sind.
     */
    public static Optional<String> normalizePhone(String phone) {
        if (phone == null) {
            return Optional.empty();
        }
        String digits = phone.replaceAll("\\D", "");
        return digits.length() < 7 ? Optional.empty()
                : Optional.of(digits.substring(Math.max(0, digits.length() - 9)));
    }

    private static String capitalize(String value) {
        return value.substring(0, 1).toUpperCase(Locale.GERMAN) + value.substring(1);
    }

    /**
     * Vor- und Nachname.
     */
    public record Name(String firstName, String lastName) {

        /**
         * {@code true}, wenn Vor- und Nachname tatsächlich ermittelt wurden (kein Platzhalter).
         */
        public boolean isKnown() {
            return !UNKNOWN_LAST_NAME.equals(lastName);
        }
    }
}
