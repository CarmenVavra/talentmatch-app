package de.talentmatch.analysis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import de.talentmatch.common.exception.BusinessRuleException;
import de.talentmatch.common.persistence.StringListConverter;

/**
 * KI-Evaluierung einer Bewerbung: Match-Score, Teil-Scores, Pro/Contra, Risiken und Interviewfragen.
 */
@Entity
@Table(name = "analysis_result")
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int overallScore;

    private int skillsScore;

    private int experienceScore;

    private int educationScore;

    private int languagesScore;

    @Column(length = 4000)
    private String summary;

    @Convert(converter = StringListConverter.class)
    @Column(length = 4000)
    private List<String> matchingSkills = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(length = 4000)
    private List<String> missingSkills = new ArrayList<>();

    /** Teilmenge der passenden Skills, die nur das Skill-Profil (Selbstauskunft) belegt. */
    @Convert(converter = StringListConverter.class)
    @Column(length = 4000)
    private List<String> skillsFromSkillProfile = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(length = 4000)
    private List<String> riskFactors = new ArrayList<>();

    @Convert(converter = InterviewQuestionListConverter.class)
    @Column(length = 20_000)
    private List<InterviewQuestion> suggestedQuestions = new ArrayList<>();

    private String model;

    private Instant analyzedAt;

    protected AnalysisResult() {
    }

    public AnalysisResult(Scores scores, String summary, List<String> matchingSkills, List<String> missingSkills,
                          List<String> riskFactors, List<InterviewQuestion> suggestedQuestions, String model,
                          Instant analyzedAt) {
        this.overallScore = scores.overall();
        this.skillsScore = scores.skills();
        this.experienceScore = scores.experience();
        this.educationScore = scores.education();
        this.languagesScore = scores.languages();
        this.summary = summary;
        this.matchingSkills = new ArrayList<>(matchingSkills);
        this.missingSkills = new ArrayList<>(missingSkills);
        this.riskFactors = new ArrayList<>(riskFactors);
        this.suggestedQuestions = new ArrayList<>(suggestedQuestions);
        this.model = model;
        this.analyzedAt = analyzedAt;
    }

    /**
     * Markiert passende Skills, die nur im Skill-Profil stehen (für HR als „laut Skill-Profil“ sichtbar).
     */
    public void markFromSkillProfile(List<String> skills) {
        this.skillsFromSkillProfile = new ArrayList<>(skills);
    }

    public void markQuestion(int index, boolean discussed) {
        if (index < 0 || index >= suggestedQuestions.size()) {
            throw new BusinessRuleException("Interviewfrage " + index + " existiert nicht.");
        }
        List<InterviewQuestion> updated = new ArrayList<>(suggestedQuestions);
        updated.set(index, updated.get(index).withDiscussed(discussed));
        this.suggestedQuestions = updated;
    }

    public void addQuestions(List<InterviewQuestion> questions) {
        List<InterviewQuestion> updated = new ArrayList<>(suggestedQuestions);
        updated.addAll(questions);
        this.suggestedQuestions = updated;
    }

    public Long getId() {
        return id;
    }

    public Scores getScores() {
        return new Scores(overallScore, skillsScore, experienceScore, educationScore, languagesScore);
    }

    public int getOverallScore() {
        return overallScore;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getMatchingSkills() {
        return List.copyOf(matchingSkills);
    }

    public List<String> getMissingSkills() {
        return List.copyOf(missingSkills);
    }

    public List<String> getSkillsFromSkillProfile() {
        return skillsFromSkillProfile == null ? List.of() : List.copyOf(skillsFromSkillProfile);
    }

    public List<String> getRiskFactors() {
        return List.copyOf(riskFactors);
    }

    public List<InterviewQuestion> getSuggestedQuestions() {
        return List.copyOf(suggestedQuestions);
    }

    public String getModel() {
        return model;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    /**
     * Gesamt- und Teil-Scores (jeweils 0–100).
     */
    public record Scores(int overall, int skills, int experience, int education, int languages) {
    }
}
