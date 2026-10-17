package de.talentmatch.resume;

import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

/**
 * Unterlagen einer Portal-Bewerbung: Lebenslauf (Pflicht), Skill-Profil und Anschreiben (optional).
 */
public record ApplicantDocuments(MultipartFile resume, Optional<MultipartFile> skillProfile,
                                 Optional<MultipartFile> coverLetter) {

    public static ApplicantDocuments of(MultipartFile resume, MultipartFile skillProfile, MultipartFile coverLetter) {
        return new ApplicantDocuments(resume, Optional.ofNullable(skillProfile), Optional.ofNullable(coverLetter));
    }

    public static ApplicantDocuments resumeOnly(MultipartFile resume) {
        return of(resume, null, null);
    }
}
