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
    private static final Pattern NOISE_WORDS =
            Pattern.compile("(?i)\\b(lebenslauf|cv|resume|bewerbung|final|neu)\\b");

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
            return new Name("Neu", "Bewerber:in");
        }
        String first = capitalize(parts[0]);
        String last = parts.length > 1
                ? String.join(" ", Arrays.stream(parts, 1, parts.length).map(ResumeHeuristics::capitalize).toList())
                : "Bewerber:in";
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

    private static String capitalize(String value) {
        return value.substring(0, 1).toUpperCase(Locale.GERMAN) + value.substring(1);
    }

    /**
     * Vor- und Nachname.
     */
    public record Name(String firstName, String lastName) {
    }
}
