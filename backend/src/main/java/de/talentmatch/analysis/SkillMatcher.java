package de.talentmatch.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Regelbasierter Abgleich der geforderten Skills einer Stelle mit den Technologien aus dem Lebenslauf.
 * <p>
 * Ein geforderter Skill gilt als erfüllt, wenn alle seine Wort-Token (ohne Versionsnummern, Synonyme normalisiert)
 * in einer Technologie der Kandidat:in vorkommen. Beispiel: „Spring Boot 3“ erfüllt „Spring Boot“,
 * „JavaScript“ erfüllt aber nicht „Java“.
 */
@Component
public class SkillMatcher {

    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("k8s", "kubernetes"),
            Map.entry("postgres", "postgresql"),
            Map.entry("js", "javascript"),
            Map.entry("ts", "typescript"),
            Map.entry("golang", "go"),
            Map.entry("amazon", "aws"),
            Map.entry("gcp", "google"),
            Map.entry("cicd", "ci"),
            Map.entry("ms", "microsoft"));

    private static final Set<String> STOP_WORDS = Set.of("und", "and", "mit", "the", "der", "die", "das");

    public SkillMatch match(List<String> requiredSkills, List<String> candidateSkills) {
        List<Set<String>> candidateTokens = candidateSkills.stream().map(SkillMatcher::tokens).toList();
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String required : requiredSkills) {
            Set<String> requiredTokens = tokens(required);
            boolean found = !requiredTokens.isEmpty()
                    && candidateTokens.stream().anyMatch(tokens -> tokens.containsAll(requiredTokens));
            if (found) {
                matched.add(required);
            } else {
                missing.add(required);
            }
        }
        int score = requiredSkills.isEmpty() ? 100 : Math.round(100f * matched.size() / requiredSkills.size());
        return new SkillMatch(List.copyOf(matched), List.copyOf(missing), score);
    }

    static Set<String> tokens(String skill) {
        String normalized = skill.toLowerCase(Locale.ROOT)
                .replace("c++", "cpp")
                .replace("c#", "csharp")
                .replace(".net", "dotnet")
                .replace("ci/cd", "ci")
                .replace("node.js", "nodejs");
        return Arrays.stream(normalized.split("[^a-z0-9äöüß]+"))
                .filter(token -> !token.isBlank())
                .filter(token -> !token.chars().allMatch(Character::isDigit))
                .filter(token -> !STOP_WORDS.contains(token))
                .map(token -> SYNONYMS.getOrDefault(token, token))
                .collect(Collectors.toSet());
    }

    /**
     * Ergebnis des Abgleichs: erfüllte und fehlende Anforderungen sowie Skill-Score (0–100).
     */
    public record SkillMatch(List<String> matched, List<String> missing, int score) {
    }
}
