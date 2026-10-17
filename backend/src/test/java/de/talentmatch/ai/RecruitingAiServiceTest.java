package de.talentmatch.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import de.talentmatch.candidate.CandidateProfile;
import de.talentmatch.common.exception.AiAnalysisException;
import de.talentmatch.job.JobPosition;

class RecruitingAiServiceTest {

    @Test
    void parseResumeConvertsStructuredJsonIntoProfile() {
        AtomicReference<Prompt> captured = new AtomicReference<>();
        ChatModel model = prompt -> {
            captured.set(prompt);
            return response("""
                    {"firstName":"Lena","lastName":"Hoffmann","email":"lena@example.org","phone":null,
                     "city":"München","currentRole":"Senior Backend Engineer","currentCompany":"FinCore",
                     "positions":[{"period":"03/2025 – heute","role":"Senior Backend Engineer",
                       "company":"FinCore GmbH","location":"München","highlights":["Microservices"]}],
                     "technologies":["Java","Spring Boot"],"education":"M.Sc. Informatik",
                     "languages":["Deutsch","Englisch (C1)"],"yearsOfExperience":7}
                    """);
        };
        RecruitingAiService service = new RecruitingAiService(ChatClient.builder(model), "sk-test", "gpt-4o");

        CandidateProfile profile = service.parseResume("Lena Hoffmann, Java-Entwicklerin {mit geschweiften}");

        assertThat(profile.firstName()).isEqualTo("Lena");
        assertThat(profile.technologies()).containsExactly("Java", "Spring Boot");
        assertThat(profile.positions()).singleElement()
                .satisfies(p -> assertThat(p.highlights()).containsExactly("Microservices"));
        assertThat(captured.get().getContents()).contains("geschweiften").contains("firstName");
    }

    @Test
    void failsFastWhenNoApiKeyIsConfigured() {
        ChatModel model = prompt -> {
            throw new IllegalStateException("darf nicht aufgerufen werden");
        };
        RecruitingAiService service = new RecruitingAiService(ChatClient.builder(model), "not-configured", "gpt-4o");

        assertThatThrownBy(() -> service.parseResume("Text"))
                .isInstanceOf(AiAnalysisException.class)
                .hasMessageContaining("OPENAI_API_KEY");
    }

    @Test
    void wrapsModelErrorsIntoAiAnalysisException() {
        ChatModel model = prompt -> {
            throw new IllegalStateException("HTTP 500");
        };
        RecruitingAiService service = new RecruitingAiService(ChatClient.builder(model), "sk-test", "gpt-4o");

        CandidateProfile profile = new CandidateProfile("A", "B", null, null, null, null, null, List.of(), List.of(),
                null, List.of(), null);
        JobPosition job = new JobPosition("Titel", "Data", "Mid", "Berlin", null, List.of("SQL"),
                LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> service.generateMoreQuestions(profile, job, List.of()))
                .isInstanceOf(AiAnalysisException.class)
                .hasMessageContaining("fehlgeschlagen");
    }

    private static ChatResponse response(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }
}
