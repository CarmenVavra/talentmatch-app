package de.talentmatch.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

import de.talentmatch.analysis.AnalysisResult;
import de.talentmatch.analysis.InterviewQuestion;
import de.talentmatch.application.api.ApplicationDetailDto;
import de.talentmatch.application.api.ApplicationDetailDto.AnalysisDto;
import de.talentmatch.application.api.ApplicationDetailDto.CandidateDetailDto;
import de.talentmatch.application.api.ApplicationDetailDto.InterviewQuestionDto;
import de.talentmatch.application.api.ApplicationDetailDto.JobRefDto;
import de.talentmatch.application.api.ApplicationSummaryDto;
import de.talentmatch.application.api.CandidateSummaryDto;
import de.talentmatch.candidate.Candidate;
import de.talentmatch.job.JobPosition;
import de.talentmatch.resume.DocumentType;

/**
 * Wandelt Entities in API-DTOs um (Entities verlassen nie die Service-Schicht).
 */
public final class ApplicationMapper {

    private ApplicationMapper() {
    }

    public static ApplicationSummaryDto toSummary(Application application) {
        Candidate candidate = application.getCandidate();
        Integer score = application.getAnalysis().map(AnalysisResult::getOverallScore).orElse(null);
        List<String> matching = application.getAnalysis().map(AnalysisResult::getMatchingSkills).orElse(List.of());
        return new ApplicationSummaryDto(
                application.getId(),
                application.getJobPosition().getId(),
                application.getJobPosition().getTitle(),
                application.getStatus(),
                application.getCreatedAt(),
                new CandidateSummaryDto(candidate.getId(), candidate.getFirstName(), candidate.getLastName(),
                        candidate.getCurrentRole(), candidate.getCurrentCompany(), candidate.getCity()),
                score,
                mergeSkills(matching, candidate.skills()));
    }

    public static ApplicationDetailDto toDetail(Application application) {
        JobPosition job = application.getJobPosition();
        Candidate candidate = application.getCandidate();
        return new ApplicationDetailDto(
                application.getId(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getInterviewNotes(),
                application.getResumeFileName().orElse(null),
                application.getDocumentFileName(DocumentType.SKILL_PROFILE).orElse(null),
                application.getDocumentFileName(DocumentType.COVER_LETTER).orElse(null),
                new JobRefDto(job.getId(), job.getTitle(), job.getDepartment(), job.getExperienceLevel(),
                        job.getLocation(), job.getRequiredSkills()),
                new CandidateDetailDto(candidate.getId(), candidate.getFirstName(), candidate.getLastName(),
                        candidate.getEmail(), candidate.getPhone(), candidate.getCity(), candidate.getCurrentRole(),
                        candidate.getCurrentCompany(), candidate.getParsedProfile().orElse(null)),
                application.getAnalysis().map(ApplicationMapper::toAnalysis).orElse(null));
    }

    private static AnalysisDto toAnalysis(AnalysisResult result) {
        AnalysisResult.Scores scores = result.getScores();
        List<InterviewQuestion> questions = result.getSuggestedQuestions();
        List<InterviewQuestionDto> questionDtos = IntStream.range(0, questions.size())
                .mapToObj(i -> new InterviewQuestionDto(i, questions.get(i).topic(), questions.get(i).text(),
                        questions.get(i).discussed()))
                .toList();
        return new AnalysisDto(scores.overall(), scores.skills(), scores.experience(), scores.education(),
                scores.languages(), result.getSummary(), result.getMatchingSkills(), result.getMissingSkills(),
                result.getSkillsFromSkillProfile(), result.getRiskFactors(), questionDtos, result.getModel(),
                result.getAnalyzedAt());
    }

    /**
     * Passende Skills zuerst, danach weitere Technologien – ohne Duplikate (Groß-/Kleinschreibung egal).
     */
    static List<String> mergeSkills(List<String> first, List<String> second) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (String skill : first) {
            merged.putIfAbsent(skill.toLowerCase(Locale.ROOT), skill);
        }
        for (String skill : second) {
            merged.putIfAbsent(skill.toLowerCase(Locale.ROOT), skill);
        }
        return new ArrayList<>(merged.values());
    }
}
