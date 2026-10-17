package de.talentmatch.application;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import de.talentmatch.analysis.AnalysisResult;
import de.talentmatch.auth.UserAccount;
import de.talentmatch.candidate.Candidate;
import de.talentmatch.job.JobPosition;
import de.talentmatch.resume.DocumentType;
import de.talentmatch.resume.ResumeDocument;

/**
 * Bewerbung einer Kandidat:in auf eine Stelle – Karte im Kanban-Board.
 */
@Entity
@Table(name = "application", uniqueConstraints = @UniqueConstraint(
        name = "uk_application_job_applicant", columnNames = {"job_position_id", "applicant_account_id"}))
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_position_id", nullable = false)
    private JobPosition jobPosition;

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private Candidate candidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplicationStatus status = ApplicationStatus.NEW;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "analysis_result_id", unique = true)
    private AnalysisResult analysis;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "resume_document_id", unique = true)
    private ResumeDocument resumeDocument;

    /** Dateiname des Original-PDFs (redundant gespeichert, damit das PDF für Listen nicht geladen wird). */
    private String resumeFileName;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "skill_profile_document_id", unique = true)
    private ResumeDocument skillProfileDocument;

    private String skillProfileFileName;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cover_letter_document_id", unique = true)
    private ResumeDocument coverLetterDocument;

    private String coverLetterFileName;

    @Column(length = 10_000)
    private String interviewNotes;

    /** Konto der Bewerber:in, falls die Bewerbung über das Portal eingereicht wurde (sonst HR-Upload). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_account_id")
    private UserAccount applicantAccount;

    /** Zeitpunkt, zu dem HR die Bewerbung erstmals geöffnet oder bearbeitet hat. */
    private LocalDateTime seenByHrAt;

    protected Application() {
    }

    public Application(JobPosition jobPosition, Candidate candidate, LocalDateTime createdAt) {
        this.jobPosition = jobPosition;
        this.candidate = candidate;
        this.createdAt = createdAt;
    }

    /**
     * Hinterlegt ein (neues) Analyse-Ergebnis. Eine neu eingegangene Bewerbung wandert dabei nach „KI-analysiert“.
     */
    public void applyAnalysis(AnalysisResult result) {
        this.analysis = result;
        if (status == ApplicationStatus.NEW) {
            status = ApplicationStatus.MATCHED;
        }
    }

    /**
     * Merkt sich, dass HR die Bewerbung gesehen hat (nur beim ersten Mal).
     */
    public void markSeenByHr(LocalDateTime now) {
        if (seenByHrAt == null) {
            seenByHrAt = now;
        }
    }

    /**
     * Neu = über das Portal eingegangen, von HR noch nicht geöffnet und noch nicht weiterbearbeitet.
     * Prüft nur den Fremdschlüssel des Kontos, lädt es also nicht nach.
     */
    public boolean isUnread() {
        return applicantAccount != null && seenByHrAt == null
                && (status == ApplicationStatus.NEW || status == ApplicationStatus.MATCHED);
    }

    public Long getId() {
        return id;
    }

    public JobPosition getJobPosition() {
        return jobPosition;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Optional<AnalysisResult> getAnalysis() {
        return Optional.ofNullable(analysis);
    }

    public Optional<ResumeDocument> getResumeDocument() {
        return getDocument(DocumentType.RESUME);
    }

    public void setResumeDocument(ResumeDocument resumeDocument) {
        setDocument(DocumentType.RESUME, resumeDocument);
    }

    public Optional<String> getResumeFileName() {
        return getDocumentFileName(DocumentType.RESUME);
    }

    public Optional<ResumeDocument> getDocument(DocumentType type) {
        return Optional.ofNullable(switch (type) {
            case RESUME -> resumeDocument;
            case SKILL_PROFILE -> skillProfileDocument;
            case COVER_LETTER -> coverLetterDocument;
        });
    }

    /**
     * Hinterlegt ein Original-PDF und merkt sich dessen Dateinamen für Listen und Detailansicht.
     */
    public void setDocument(DocumentType type, ResumeDocument document) {
        String fileName = document == null ? null : document.getFileName();
        switch (type) {
            case RESUME -> {
                this.resumeDocument = document;
                this.resumeFileName = fileName;
            }
            case SKILL_PROFILE -> {
                this.skillProfileDocument = document;
                this.skillProfileFileName = fileName;
            }
            case COVER_LETTER -> {
                this.coverLetterDocument = document;
                this.coverLetterFileName = fileName;
            }
        }
    }

    public Optional<String> getDocumentFileName(DocumentType type) {
        return Optional.ofNullable(switch (type) {
            case RESUME -> resumeFileName;
            case SKILL_PROFILE -> skillProfileFileName;
            case COVER_LETTER -> coverLetterFileName;
        });
    }

    public Optional<UserAccount> getApplicantAccount() {
        return Optional.ofNullable(applicantAccount);
    }

    public void setApplicantAccount(UserAccount applicantAccount) {
        this.applicantAccount = applicantAccount;
    }

    public String getInterviewNotes() {
        return interviewNotes;
    }

    public void setInterviewNotes(String interviewNotes) {
        this.interviewNotes = interviewNotes;
    }
}
