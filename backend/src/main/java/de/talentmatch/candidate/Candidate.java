package de.talentmatch.candidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import de.talentmatch.common.persistence.StringListConverter;

/**
 * Bewerber:in mit Rohtext des Lebenslaufs und dem per KI extrahierten Profil.
 */
@Entity
@Table(name = "candidate")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String email;

    private String phone;

    private String city;

    /** {@code current_role} ist in SQL reserviert – daher eigener Spaltenname. */
    @Column(name = "current_position")
    private String currentRole;

    private String currentCompany;

    @Column(length = 200_000)
    private String rawResumeText;

    @Convert(converter = CandidateProfileConverter.class)
    @Column(length = 50_000)
    private CandidateProfile parsedProfile;

    /** Text des optionalen Skill-Profils (Selbstauskunft der Bewerber:in). */
    @Column(length = 200_000)
    private String skillProfileText;

    /** Per KI aus dem Skill-Profil extrahierte Skills (leer, solange nicht analysiert). */
    @Convert(converter = StringListConverter.class)
    @Column(length = 4000)
    private List<String> skillProfileSkills = new ArrayList<>();

    /** Text des optionalen Anschreibens – fließt nur qualitativ in die Analyse ein. */
    @Column(length = 200_000)
    private String coverLetterText;

    protected Candidate() {
    }

    public Candidate(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * Übernimmt die Stammdaten aus dem extrahierten Profil (nur nicht-leere Werte).
     */
    public void applyProfile(CandidateProfile profile) {
        this.parsedProfile = profile;
        this.firstName = valueOr(profile.firstName(), firstName);
        this.lastName = valueOr(profile.lastName(), lastName);
        this.email = valueOr(profile.email(), email);
        this.phone = valueOr(profile.phone(), phone);
        this.city = valueOr(profile.city(), city);
        this.currentRole = valueOr(profile.currentRole(), currentRole);
        this.currentCompany = valueOr(profile.currentCompany(), currentCompany);
    }

    /**
     * Alle bekannten Technologien/Skills aus dem Profil.
     */
    public List<String> skills() {
        return getParsedProfile().map(CandidateProfile::technologies).orElse(List.of());
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(String currentRole) {
        this.currentRole = currentRole;
    }

    public String getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    public String getRawResumeText() {
        return rawResumeText;
    }

    public void setRawResumeText(String rawResumeText) {
        this.rawResumeText = rawResumeText;
    }

    /**
     * Neuer Lebenslauf (z. B. beim Zusammenführen): das bisher extrahierte Profil gilt nicht mehr.
     */
    public void replaceResumeText(String resumeText) {
        this.rawResumeText = resumeText;
        this.parsedProfile = null;
    }

    /**
     * Neues Skill-Profil: bereits extrahierte Skills werden bei der nächsten Analyse neu ermittelt.
     */
    public void replaceSkillProfileText(String skillProfileText) {
        this.skillProfileText = skillProfileText;
        this.skillProfileSkills = new ArrayList<>();
    }

    public Optional<CandidateProfile> getParsedProfile() {
        return Optional.ofNullable(parsedProfile);
    }

    public Optional<String> getSkillProfileText() {
        return Optional.ofNullable(skillProfileText).filter(text -> !text.isBlank());
    }

    public void setSkillProfileText(String skillProfileText) {
        this.skillProfileText = skillProfileText;
    }

    public List<String> getSkillProfileSkills() {
        return skillProfileSkills == null ? List.of() : List.copyOf(skillProfileSkills);
    }

    public void setSkillProfileSkills(List<String> skillProfileSkills) {
        this.skillProfileSkills = new ArrayList<>(skillProfileSkills);
    }

    public Optional<String> getCoverLetterText() {
        return Optional.ofNullable(coverLetterText).filter(text -> !text.isBlank());
    }

    public void setCoverLetterText(String coverLetterText) {
        this.coverLetterText = coverLetterText;
    }
}
