package de.talentmatch.ai;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.talentmatch.analysis.SkillMatcher.SkillMatch;
import de.talentmatch.candidate.CandidateProfile;
import de.talentmatch.common.exception.AiAnalysisException;
import de.talentmatch.job.JobPosition;

/**
 * KI-Workflows mit Spring AI. Alle Antworten werden über {@link BeanOutputConverter} als typisierte
 * Java-Objekte zurückgegeben (Structured Output).
 */
@Service
public class RecruitingAiService {

    private static final int MAX_RESUME_CHARS = 30_000;
    private static final String NOT_CONFIGURED = "not-configured";

    private static final String PARSE_SYSTEM = """
            Du bist ein präziser HR-Assistent. Extrahiere strukturierte Daten aus dem Lebenslauf.
            Erfinde keine Angaben: Unbekannte Felder sind null bzw. leere Listen.
            Technologien als kurze, gängige Begriffe (z. B. "Java", "Spring Boot", "Kubernetes").
            """;

    private static final String MATCH_SYSTEM = """
            Du bist erfahrene:r Recruiter:in. Vergleiche das Bewerber-Profil A mit dem Anforderungsprofil B.
            Der Skill-Abgleich wurde bereits regelbasiert berechnet und ist verbindlich – bewerte ergänzend
            Berufserfahrung, Ausbildung und Sprachen. Antworte ausschließlich auf Deutsch, sachlich und fair;
            berücksichtige keine Merkmale wie Alter, Geschlecht, Herkunft oder Religion.
            """;

    private final ChatClient chatClient;
    private final String apiKey;
    private final String model;

    public RecruitingAiService(ChatClient.Builder chatClientBuilder,
                               @Value("${spring.ai.openai.api-key:}") String apiKey,
                               @Value("${spring.ai.openai.chat.options.model:gpt-4o}") String model) {
        this.chatClient = chatClientBuilder.build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public String modelName() {
        return model;
    }

    /**
     * Workflow 1: Resume Parsing – Rohtext → {@link CandidateProfile}.
     */
    public CandidateProfile parseResume(String resumeText) {
        String text = resumeText.length() > MAX_RESUME_CHARS ? resumeText.substring(0, MAX_RESUME_CHARS) : resumeText;
        BeanOutputConverter<CandidateProfile> converter = new BeanOutputConverter<>(CandidateProfile.class);
        String user = "Lebenslauf:\n\"\"\"\n" + text + "\n\"\"\"\n\n" + converter.getFormat();
        return call(PARSE_SYSTEM, user, converter);
    }

    /**
     * Workflow 2: Match Analysis &amp; Scoring – Profil + Anforderungen → {@link MatchAssessment}.
     */
    public MatchAssessment assessMatch(CandidateProfile profile, JobPosition job, SkillMatch skillMatch) {
        BeanOutputConverter<MatchAssessment> converter = new BeanOutputConverter<>(MatchAssessment.class);
        String user = describeJob(job) + "\n\n" + describeProfile(profile) + "\n\n"
                + "Regelbasierter Skill-Abgleich:\n"
                + "- erfüllt: " + String.join(", ", skillMatch.matched()) + "\n"
                + "- fehlend: " + String.join(", ", skillMatch.missing()) + "\n\n"
                + "Erstelle Management Summary, Risikofaktoren und 4-5 Interviewfragen "
                + "(mindestens eine zu jedem fehlenden Kernskill).\n\n"
                + converter.getFormat();
        return call(MATCH_SYSTEM, user, converter);
    }

    /**
     * Erzeugt zusätzliche Interviewfragen, die die bestehenden nicht wiederholen.
     */
    public List<QuestionSuggestion> generateMoreQuestions(CandidateProfile profile, JobPosition job,
                                                          List<String> existingQuestions) {
        BeanOutputConverter<QuestionBatch> converter = new BeanOutputConverter<>(QuestionBatch.class);
        String user = describeJob(job) + "\n\n" + describeProfile(profile) + "\n\n"
                + "Bereits vorhandene Fragen:\n"
                + existingQuestions.stream().map(q -> "- " + q).collect(Collectors.joining("\n")) + "\n\n"
                + "Formuliere 3 weitere, neue Interviewfragen.\n\n"
                + converter.getFormat();
        return call(MATCH_SYSTEM, user, converter).questions();
    }

    private <T> T call(String system, String user, BeanOutputConverter<T> converter) {
        ensureConfigured();
        try {
            T result = chatClient.prompt()
                    .messages(new SystemMessage(system), new UserMessage(user))
                    .call()
                    .entity(converter);
            if (result == null) {
                throw new AiAnalysisException("Das Sprachmodell hat keine verwertbare Antwort geliefert.");
            }
            return result;
        } catch (AiAnalysisException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AiAnalysisException("Die KI-Analyse ist fehlgeschlagen. Bitte erneut versuchen.", ex);
        }
    }

    private void ensureConfigured() {
        if (apiKey == null || apiKey.isBlank() || NOT_CONFIGURED.equals(apiKey)) {
            throw new AiAnalysisException(
                    "Kein OpenAI-API-Key konfiguriert. Bitte die Umgebungsvariable OPENAI_API_KEY setzen.");
        }
    }

    private static String describeJob(JobPosition job) {
        return "Anforderungsprofil B (Stelle):\n"
                + "- Titel: " + job.getTitle() + "\n"
                + "- Abteilung: " + job.getDepartment() + "\n"
                + "- Level: " + job.getExperienceLevel() + "\n"
                + "- Ort: " + job.getLocation() + "\n"
                + "- Geforderte Skills: " + String.join(", ", job.getRequiredSkills()) + "\n"
                + "- Beschreibung: " + (job.getDescription() == null ? "-" : job.getDescription());
    }

    private static String describeProfile(CandidateProfile profile) {
        String positions = profile.positions().stream()
                .map(p -> "  * " + p.period() + ": " + p.role() + " bei " + p.company()
                        + (p.highlights().isEmpty() ? "" : " (" + String.join("; ", p.highlights()) + ")"))
                .collect(Collectors.joining("\n"));
        return "Bewerber-Profil A:\n"
                + "- Aktuelle Position: " + profile.currentRole() + " · " + profile.currentCompany() + "\n"
                + "- Berufserfahrung (Jahre): " + profile.yearsOfExperience() + "\n"
                + "- Stationen:\n" + positions + "\n"
                + "- Technologien: " + String.join(", ", profile.technologies()) + "\n"
                + "- Ausbildung: " + profile.education() + "\n"
                + "- Sprachen: " + String.join(", ", profile.languages());
    }
}
