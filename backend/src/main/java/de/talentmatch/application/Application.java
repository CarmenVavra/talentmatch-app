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

import de.talentmatch.analysis.AnalysisResult;
import de.talentmatch.candidate.Candidate;
import de.talentmatch.job.JobPosition;
import de.talentmatch.resume.ResumeDocument;

/**
 * Bewerbung einer Kandidat:in auf eine Stelle – Karte im Kanban-Board.
 */
@Entity
@Table(name = "application")
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

    @Column(length = 10_000)
    private String interviewNotes;

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
        return Optional.ofNullable(resumeDocument);
    }

    public void setResumeDocument(ResumeDocument resumeDocument) {
        this.resumeDocument = resumeDocument;
        this.resumeFileName = resumeDocument == null ? null : resumeDocument.getFileName();
    }

    public Optional<String> getResumeFileName() {
        return Optional.ofNullable(resumeFileName);
    }

    public String getInterviewNotes() {
        return interviewNotes;
    }

    public void setInterviewNotes(String interviewNotes) {
        this.interviewNotes = interviewNotes;
    }
}
