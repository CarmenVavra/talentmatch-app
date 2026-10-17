package de.talentmatch.candidate;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    public Optional<CandidateProfile> getParsedProfile() {
        return Optional.ofNullable(parsedProfile);
    }
}
