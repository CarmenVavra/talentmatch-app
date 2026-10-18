package de.talentmatch.resume;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Erkennt anhand von Dateiname und Text, ob ein PDF ein Lebenslauf, ein Skill-Profil oder ein Anschreiben ist.
 * Der Dateiname hat Vorrang; sonst entscheiden typische Formulierungen. Im Zweifel: Lebenslauf.
 */
public final class DocumentClassifier {

    private static final Pattern COVER_LETTER_NAME =
            Pattern.compile("anschreiben|motivation|cover|letter|bewerbungsschreiben");
    private static final Pattern SKILL_PROFILE_NAME =
            Pattern.compile("skill|kompetenz|qualifikation|profil(?!e?bild)");
    private static final Pattern RESUME_NAME = Pattern.compile("lebenslauf|\\bcv\\b|resume|vita");

    private static final List<String> COVER_LETTER_SIGNALS = List.of("sehr geehrte", "mit freundlichen grüßen",
            "mit freundlichen gruessen", "hiermit bewerbe ich", "bewerbe mich", "dear ", "kind regards");
    private static final List<String> SKILL_SIGNALS = List.of("skill", "kenntnisse", "kompetenzen",
            "programmiersprachen", "frameworks", "technologien", "tools", "methoden");
    private static final List<String> RESUME_SIGNALS = List.of("berufserfahrung", "werdegang", "ausbildung",
            "studium", "lebenslauf", "beruflicher", "work experience", "education");

    private DocumentClassifier() {
    }

    public static DocumentType classify(String fileName, String text) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.GERMAN).replace('_', ' ');
        if (COVER_LETTER_NAME.matcher(name).find()) {
            return DocumentType.COVER_LETTER;
        }
        if (SKILL_PROFILE_NAME.matcher(name).find()) {
            return DocumentType.SKILL_PROFILE;
        }
        if (RESUME_NAME.matcher(name).find()) {
            return DocumentType.RESUME;
        }
        return classifyText(text);
    }

    static DocumentType classifyText(String text) {
        String content = text == null ? "" : text.toLowerCase(Locale.GERMAN);
        long cover = count(content, COVER_LETTER_SIGNALS);
        long resume = count(content, RESUME_SIGNALS);
        long skills = count(content, SKILL_SIGNALS);
        if (cover >= 2 && cover >= resume) {
            return DocumentType.COVER_LETTER;
        }
        if (skills >= 2 && resume <= 1) {
            return DocumentType.SKILL_PROFILE;
        }
        return DocumentType.RESUME;
    }

    private static long count(String content, List<String> signals) {
        return signals.stream().filter(content::contains).count();
    }
}
