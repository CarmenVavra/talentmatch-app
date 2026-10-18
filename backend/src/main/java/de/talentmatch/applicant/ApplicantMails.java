package de.talentmatch.applicant;

/**
 * Texte der E-Mails an Bewerber:innen mit Portal-Konto: Eingangsbestätigung und Statusänderungen.
 */
final class ApplicantMails {

    private ApplicantMails() {
    }

    record Mail(String subject, String body) {
    }

    static Mail received(String firstName, String lastName, String jobTitle, String portalUrl) {
        return new Mail("Ihre Bewerbung als " + jobTitle + " ist eingegangen", letter(firstName, lastName, """
                vielen Dank für Ihre Bewerbung als „%s“. Ihre Unterlagen sind bei uns eingegangen und werden \
                jetzt sorgfältig geprüft. Wir melden uns, sobald es Neuigkeiten gibt.""".formatted(jobTitle),
                portalUrl));
    }

    /**
     * Mail zum neuen, für Bewerber:innen sichtbaren Status.
     */
    static Mail statusChanged(ApplicantStatus status, String firstName, String lastName, String jobTitle,
                              String portalUrl) {
        return switch (status) {
            case RECEIVED -> new Mail("Ihre Bewerbung als " + jobTitle + " wird geprüft",
                    letter(firstName, lastName, """
                            Ihre Bewerbung als „%s“ befindet sich wieder in Prüfung. Wir melden uns, sobald es \
                            Neuigkeiten gibt.""".formatted(jobTitle), portalUrl));
            case INTERVIEW -> new Mail("Einladung zum Kennenlernen: " + jobTitle,
                    letter(firstName, lastName, """
                            Ihre Bewerbung als „%s“ hat uns überzeugt und wir möchten Sie gerne persönlich \
                            kennenlernen. Unser HR-Team meldet sich in den nächsten Tagen mit einem \
                            Terminvorschlag bei Ihnen.""".formatted(jobTitle), portalUrl));
            case OFFER -> new Mail("Zusage: Ihre Bewerbung als " + jobTitle,
                    letter(firstName, lastName, """
                            wir freuen uns sehr, Ihnen mitteilen zu können, dass wir Ihnen die Position „%s“ \
                            anbieten möchten. Unser HR-Team meldet sich in Kürze mit den nächsten Schritten bei \
                            Ihnen.""".formatted(jobTitle), portalUrl));
            case REJECTED -> new Mail("Ihre Bewerbung als " + jobTitle,
                    letter(firstName, lastName, """
                            vielen Dank für Ihr Interesse an der Position „%s“ und die Zeit, die Sie in Ihre \
                            Bewerbung investiert haben. Nach sorgfältiger Prüfung haben wir uns für eine andere \
                            Bewerbung entschieden. Wir wissen, dass diese Nachricht enttäuschend ist, und \
                            wünschen Ihnen für Ihren weiteren Weg alles Gute.""".formatted(jobTitle), portalUrl));
        };
    }

    private static String letter(String firstName, String lastName, String text, String portalUrl) {
        return """
                Guten Tag %s %s,

                %s

                Den aktuellen Stand Ihrer Bewerbung sehen Sie jederzeit im Bewerber-Portal:
                %s

                Mit freundlichen Grüßen
                Ihr Recruiting-Team""".formatted(firstName, lastName, text, portalUrl);
    }
}
