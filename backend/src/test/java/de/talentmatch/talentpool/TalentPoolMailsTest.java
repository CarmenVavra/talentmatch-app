package de.talentmatch.talentpool;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class TalentPoolMailsTest {

    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 11, 1, 9, 0);

    @Test
    void requestBodyIsPoliteAndContainsTheLinkPlaceholder() {
        String body = TalentPoolMails.requestBody("Carmen", "Gregor", "Fullstack-Entwickler:in", "Katrin Vogel");

        assertThat(body).startsWith("Guten Tag Carmen Gregor,")
                .contains("„Fullstack-Entwickler:in“")
                .contains("in Evidenz halten")
                .contains(TalentPoolMails.LINKS_MARKER)
                .endsWith("Mit freundlichen Grüßen\nKatrin Vogel");
    }

    @Test
    void replacesThePlaceholderWithPersonalLinksAndDeadline() {
        String body = TalentPoolMails.withLinks("Hallo\n\n[Antwort-Links]\n\nGruß",
                "http://localhost:4200/evidenz/abc", DEADLINE, 24);

        assertThat(body).doesNotContain(TalentPoolMails.LINKS_MARKER)
                .contains("bis zum 01.11.2026")
                .contains("http://localhost:4200/evidenz/abc?antwort=ja")
                .contains("http://localhost:4200/evidenz/abc?antwort=nein")
                .contains("höchstens 24 Monate")
                .endsWith("Gruß");
    }

    @Test
    void appendsTheLinksWhenHrRemovedThePlaceholder() {
        String body = TalentPoolMails.withLinks("Eigener Text", "http://x/evidenz/abc", DEADLINE, 24);

        assertThat(body).startsWith("Eigener Text\n\n").contains("?antwort=ja");
    }
}
