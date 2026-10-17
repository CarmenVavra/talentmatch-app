package de.talentmatch.resume;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResumeHeuristicsTest {

    @Test
    void guessesNameFromTypicalFileNames() {
        assertThat(ResumeHeuristics.guessName("Lebenslauf_Maria_Schulz.pdf"))
                .isEqualTo(new ResumeHeuristics.Name("Maria", "Schulz"));
        assertThat(ResumeHeuristics.guessName("CV-ben-koch-2026.PDF"))
                .isEqualTo(new ResumeHeuristics.Name("Ben", "Koch"));
    }

    @Test
    void fallsBackToPlaceholderName() {
        assertThat(ResumeHeuristics.guessName("lebenslauf.pdf"))
                .isEqualTo(new ResumeHeuristics.Name("Neu", "Bewerber:in"));
        assertThat(ResumeHeuristics.guessName(null)).isEqualTo(new ResumeHeuristics.Name("Neu", "Bewerber:in"));
    }

    @Test
    void findsEmailAndPhoneInText() {
        String text = "Maria Schulz\nmaria.schulz@example.org · +49 151 2384 1234 · Berlin";

        assertThat(ResumeHeuristics.findEmail(text)).contains("maria.schulz@example.org");
        assertThat(ResumeHeuristics.findPhone(text)).contains("+49 151 2384 1234");
        assertThat(ResumeHeuristics.findEmail("keine Kontaktdaten")).isEmpty();
    }
}
