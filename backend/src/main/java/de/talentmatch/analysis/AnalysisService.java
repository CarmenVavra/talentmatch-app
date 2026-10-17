package de.talentmatch.analysis;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import de.talentmatch.ai.MatchAssessment;
import de.talentmatch.ai.QuestionSuggestion;
import de.talentmatch.ai.RecruitingAiService;
import de.talentmatch.analysis.SkillMatcher.SkillMatch;
import de.talentmatch.application.Application;
import de.talentmatch.application.ApplicationMapper;
import de.talentmatch.application.ApplicationRepository;
import de.talentmatch.application.api.ApplicationDetailDto;
import de.talentmatch.candidate.Candidate;
import de.talentmatch.candidate.CandidateProfile;
import de.talentmatch.common.exception.BusinessRuleException;
import de.talentmatch.common.exception.ResourceNotFoundException;

/**
 * Orchestriert die KI-Analyse einer Bewerbung: Resume Parsing → Skill-Matching → LLM-Bewertung.
 * <p>
 * Die (langsamen) LLM-Aufrufe laufen bewusst außerhalb einer Datenbank-Transaktion; gespeichert wird danach
 * in einer kurzen Transaktion.
 */
@Service
public class AnalysisService {

    private static final double WEIGHT_SKILLS = 0.5;
    private static final double WEIGHT_EXPERIENCE = 0.3;
    private static final double WEIGHT_EDUCATION = 0.1;
    private static final double WEIGHT_LANGUAGES = 0.1;

    private final ApplicationRepository applicationRepository;
    private final RecruitingAiService aiService;
    private final SkillMatcher skillMatcher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public AnalysisService(ApplicationRepository applicationRepository, RecruitingAiService aiService,
                           SkillMatcher skillMatcher, TransactionTemplate transactionTemplate, Clock clock) {
        this.applicationRepository = applicationRepository;
        this.aiService = aiService;
        this.skillMatcher = skillMatcher;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    public ApplicationDetailDto analyze(Long applicationId) {
        Application snapshot = load(applicationId);
        Candidate candidate = snapshot.getCandidate();
        boolean needsParsing = candidate.getParsedProfile().isEmpty();
        CandidateProfile profile = candidate.getParsedProfile().orElseGet(() -> parse(candidate));

        SkillMatch skillMatch = skillMatcher.match(snapshot.getJobPosition().getRequiredSkills(),
                profile.technologies());
        MatchAssessment assessment = aiService.assessMatch(profile, snapshot.getJobPosition(), skillMatch);
        AnalysisResult result = toResult(skillMatch, assessment);

        return transactionTemplate.execute(status -> {
            Application application = load(applicationId);
            if (needsParsing) {
                application.getCandidate().applyProfile(profile);
            }
            application.applyAnalysis(result);
            applicationRepository.flush();
            return ApplicationMapper.toDetail(application);
        });
    }

    public ApplicationDetailDto generateMoreQuestions(Long applicationId) {
        Application snapshot = load(applicationId);
        AnalysisResult analysis = snapshot.getAnalysis()
                .orElseThrow(() -> new BusinessRuleException("Bitte zuerst die KI-Analyse starten."));
        CandidateProfile profile = snapshot.getCandidate().getParsedProfile()
                .orElseThrow(() -> new BusinessRuleException("Für diese Bewerbung liegt kein Profil vor."));
        List<String> existing = analysis.getSuggestedQuestions().stream().map(InterviewQuestion::text).toList();
        List<InterviewQuestion> added = toQuestions(
                aiService.generateMoreQuestions(profile, snapshot.getJobPosition(), existing));

        return transactionTemplate.execute(status -> {
            Application application = load(applicationId);
            application.getAnalysis().ifPresent(current -> current.addQuestions(added));
            applicationRepository.flush();
            return ApplicationMapper.toDetail(application);
        });
    }

    private CandidateProfile parse(Candidate candidate) {
        String text = candidate.getRawResumeText();
        if (text == null || text.isBlank()) {
            throw new BusinessRuleException("Für diese Bewerbung liegt kein Lebenslauf-Text vor.");
        }
        return aiService.parseResume(text);
    }

    private AnalysisResult toResult(SkillMatch skillMatch, MatchAssessment assessment) {
        int skills = skillMatch.score();
        int experience = clamp(assessment.experienceScore());
        int education = clamp(assessment.educationScore());
        int languages = clamp(assessment.languagesScore());
        int overall = clamp((int) Math.round(skills * WEIGHT_SKILLS + experience * WEIGHT_EXPERIENCE
                + education * WEIGHT_EDUCATION + languages * WEIGHT_LANGUAGES));
        return new AnalysisResult(
                new AnalysisResult.Scores(overall, skills, experience, education, languages),
                assessment.summary(),
                skillMatch.matched(),
                skillMatch.missing(),
                assessment.riskFactors(),
                toQuestions(assessment.interviewQuestions()),
                aiService.modelName(),
                clock.instant());
    }

    private static List<InterviewQuestion> toQuestions(List<QuestionSuggestion> suggestions) {
        return suggestions.stream()
                .filter(s -> s.question() != null && !s.question().isBlank())
                .map(s -> new InterviewQuestion(s.topic() == null ? "Allgemein" : s.topic(), s.question(), false))
                .toList();
    }

    private Application load(Long id) {
        return applicationRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bewerbung", id));
    }

    static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
