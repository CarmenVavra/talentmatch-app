package de.talentmatch.application.api;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import de.talentmatch.application.ApplicationStatus;
import de.talentmatch.candidate.CandidateProfile;

/**
 * Bewerber-Detail (AI Analytics View): Kandidat:in, extrahiertes Profil und KI-Analyse ({@code analysis} ist null,
 * solange die Bewerbung nicht analysiert wurde). Dateinamen optionaler Unterlagen sind null, wenn nicht eingereicht.
 */
public record ApplicationDetailDto(
        Long id,
        ApplicationStatus status,
        LocalDateTime createdAt,
        String interviewNotes,
        String resumeFileName,
        String skillProfileFileName,
        String coverLetterFileName,
        JobRefDto job,
        CandidateDetailDto candidate,
        AnalysisDto analysis,
        boolean unread) {

    public record JobRefDto(
            Long id,
            String title,
            String department,
            String experienceLevel,
            String location,
            List<String> requiredSkills) {
    }

    public record CandidateDetailDto(
            Long id,
            String firstName,
            String lastName,
            String email,
            String phone,
            String city,
            String currentRole,
            String currentCompany,
            CandidateProfile profile) {
    }

    public record AnalysisDto(
            int overallScore,
            int skillsScore,
            int experienceScore,
            int educationScore,
            int languagesScore,
            String summary,
            List<String> matchingSkills,
            List<String> missingSkills,
            List<String> skillsFromSkillProfile,
            List<String> riskFactors,
            List<InterviewQuestionDto> questions,
            String model,
            Instant analyzedAt) {
    }

    public record InterviewQuestionDto(int index, String topic, String text, boolean discussed) {
    }
}
