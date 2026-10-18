package de.talentmatch.talentpool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Texte der Evidenz-E-Mails. Der Anfrage-Text ist für HR bearbeitbar; die persönlichen Antwort-Links setzt der
 * Server an der Platzhalterzeile {@link #LINKS_MARKER} ein (fehlt sie, werden die Links angehängt).
 */
final class TalentPoolMails {

    static final String LINKS_MARKER = "[Antwort-Links]";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private TalentPoolMails() {
    }

    static String requestSubject(String jobTitle) {
        return "Ihre Bewerbung als " + jobTitle;
    }

    /**
     * Bearbeitbarer Vorschlag für die Anfrage (Absage mit Bitte um Einwilligung zur Evidenz).
     */
    static String requestBody(String firstName, String lastName, String jobTitle, String signature) {
        return """
                Guten Tag %s %s,

                vielen Dank für Ihr Interesse an der Position „%s“ und für die Zeit, die Sie in Ihre Bewerbung \
                investiert haben. Nach sorgfältiger Prüfung haben wir uns für diese Position für eine andere \
                Bewerbung entschieden. Die Entscheidung ist uns nicht leichtgefallen, denn Ihr Profil hat uns \
                sehr gut gefallen.

                Deshalb würden wir Ihre Unterlagen gerne in Evidenz halten und uns bei Ihnen melden, sobald sich \
                eine passende Gelegenheit ergibt.

                %s

                Für Ihren weiteren Weg wünschen wir Ihnen alles Gute.

                Mit freundlichen Grüßen
                %s""".formatted(firstName, lastName, jobTitle, LINKS_MARKER, signature);
    }

    /**
     * Ersetzt den Platzhalter durch Antwort-Links, Frist und Hinweis zur Löschung.
     */
    static String withLinks(String body, String answerUrl, LocalDateTime deadline, long retentionMonths) {
        String links = """
                Wenn Sie damit einverstanden sind, bestätigen Sie dies bitte bis zum %s:

                Ja, gerne in Evidenz bleiben:
                %s?antwort=ja

                Nein, danke:
                %s?antwort=nein

                Ohne Ihre Bestätigung löschen wir Ihre Unterlagen nach Ablauf dieser Frist vollständig. Mit Ihrer \
                Zustimmung bewahren wir sie höchstens %d Monate auf; Sie können die Zustimmung jederzeit über \
                denselben Link widerrufen.""".formatted(DATE.format(deadline), answerUrl, answerUrl,
                retentionMonths);
        return body.contains(LINKS_MARKER) ? body.replace(LINKS_MARKER, links) : body + "\n\n" + links;
    }

    static String reminderSubject(String jobTitle) {
        return "Erinnerung: Ihre Bewerbung als " + jobTitle;
    }

    static String reminderBody(String firstName, String lastName, String jobTitle, String answerUrl,
                               LocalDateTime deadline) {
        return """
                Guten Tag %s %s,

                vor einigen Tagen haben wir Sie gefragt, ob wir Ihre Unterlagen zur Position „%s“ in Evidenz \
                halten dürfen. Wir freuen uns über Ihre kurze Rückmeldung bis zum %s:

                Ja, gerne in Evidenz bleiben:
                %s?antwort=ja

                Nein, danke:
                %s?antwort=nein

                Ohne Antwort löschen wir Ihre Unterlagen nach Ablauf der Frist vollständig.

                Mit freundlichen Grüßen
                Ihr Recruiting-Team""".formatted(firstName, lastName, jobTitle, DATE.format(deadline), answerUrl,
                answerUrl);
    }

    static String confirmationSubject() {
        return "Danke – Ihre Unterlagen bleiben in Evidenz";
    }

    static String confirmationBody(String firstName, String lastName, String answerUrl, LocalDateTime expiresAt) {
        return """
                Guten Tag %s %s,

                vielen Dank für Ihre Zustimmung. Wir halten Ihre Unterlagen bis zum %s in Evidenz und melden uns, \
                sobald sich eine passende Gelegenheit ergibt.

                Sie können Ihre Zustimmung jederzeit widerrufen – Ihre Unterlagen werden dann sofort gelöscht:
                %s

                Mit freundlichen Grüßen
                Ihr Recruiting-Team""".formatted(firstName, lastName, DATE.format(expiresAt), answerUrl);
    }
}
