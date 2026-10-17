package de.talentmatch.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.talentmatch.analysis.SkillMatcher.SkillMatch;

class SkillMatcherTest {

    private final SkillMatcher matcher = new SkillMatcher();

    @Test
    void matchesSkillsIgnoringVersionsAndCase() {
        SkillMatch result = matcher.match(
                List.of("Java", "Spring Boot", "Kubernetes", "PostgreSQL", "AWS"),
                List.of("java 17", "Spring Boot 3", "K8s", "Postgres", "Docker"));

        assertThat(result.matched()).containsExactly("Java", "Spring Boot", "Kubernetes", "PostgreSQL");
        assertThat(result.missing()).containsExactly("AWS");
        assertThat(result.score()).isEqualTo(80);
    }

    @Test
    void doesNotConfuseJavaWithJavaScript() {
        SkillMatch result = matcher.match(List.of("Java"), List.of("JavaScript", "TypeScript"));

        assertThat(result.matched()).isEmpty();
        assertThat(result.score()).isZero();
    }

    @Test
    void lessSpecificCandidateSkillDoesNotSatisfyMoreSpecificRequirement() {
        SkillMatch result = matcher.match(List.of("Spring Boot"), List.of("Spring"));

        assertThat(result.missing()).containsExactly("Spring Boot");
    }

    @Test
    void emptyRequirementsGiveFullScore() {
        assertThat(matcher.match(List.of(), List.of("Java")).score()).isEqualTo(100);
    }

    @Test
    void normalizesSpecialNotations() {
        assertThat(SkillMatcher.tokens("CI/CD")).containsExactly("ci");
        assertThat(SkillMatcher.tokens("C++")).containsExactly("cpp");
        assertThat(SkillMatcher.tokens("REST / OpenAPI")).containsExactlyInAnyOrder("rest", "openapi");
    }
}
