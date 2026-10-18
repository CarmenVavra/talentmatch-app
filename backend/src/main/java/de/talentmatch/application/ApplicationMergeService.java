package de.talentmatch.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.talentmatch.application.api.ApplicationDetailDto;
import de.talentmatch.application.api.DuplicateDto;
import de.talentmatch.application.api.DuplicateDto.DocumentRefDto;
import de.talentmatch.application.api.MergeRequest;
import de.talentmatch.application.api.MergeRequest.DocumentMove;
import de.talentmatch.auth.UserAccount;
import de.talentmatch.candidate.Candidate;
import de.talentmatch.common.exception.BusinessRuleException;
import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.resume.DocumentClassifier;
import de.talentmatch.resume.DocumentType;
import de.talentmatch.resume.ResumeDocument;
import de.talentmatch.resume.ResumeHeuristics;
import de.talentmatch.resume.api.GroupMatch;

/**
 * Erkennt mögliche Duplikate und führt zwei Bewerbungen derselben Person zusammen – nur innerhalb derselben
 * Stelle (bewerbungsübergreifend nie) und nie über verschiedene Bewerber-Konten hinweg.
 */
@Service
public class ApplicationMergeService {

    private static final String APPLICATION = "Bewerbung";

    private final ApplicationRepository applicationRepository;
    private final Clock clock;

    public ApplicationMergeService(ApplicationRepository applicationRepository, Clock clock) {
        this.applicationRepository = applicationRepository;
        this.clock = clock;
    }

    /**
     * Andere Bewerbungen derselben Stelle mit gleicher E-Mail, Telefonnummer oder gleichem Namen
     * (eine Abfrage für die ganze Stelle, Unterlagen werden nicht geladen).
     */
    @Transactional(readOnly = true)
    public List<DuplicateDto> findDuplicates(Long id) {
        Application application = applicationRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException(APPLICATION, id));
        Candidate candidate = application.getCandidate();
        List<DuplicateDto> duplicates = new ArrayList<>();
        for (Application other
                : applicationRepository.findByJobPositionIdOrderByCreatedAtDesc(application.getJobPosition().getId())) {
            if (!other.getId().equals(id) && !differentAccounts(application, other)) {
                match(candidate, other.getCandidate()).ifPresent(reason -> duplicates.add(toDuplicate(other, reason)));
            }
        }
        return duplicates;
    }

    /**
     * Übernimmt die gewählten Unterlagen, Notizen und Kontaktdaten der Quelle in diese Bewerbung und löscht die
     * Quelle. Haben sich Unterlagen geändert, wird die KI-Analyse verworfen (sie muss neu gestartet werden).
     */
    @Transactional
    public ApplicationDetailDto merge(Long targetId, MergeRequest request) {
        if (targetId.equals(request.sourceId())) {
            throw new BusinessRuleException("Eine Bewerbung kann nicht mit sich selbst zusammengeführt werden.");
        }
        Application target = loadWithDocuments(targetId);
        Application source = loadWithDocuments(request.sourceId());
        if (!target.getJobPosition().getId().equals(source.getJobPosition().getId())) {
            throw new BusinessRuleException("Nur Bewerbungen auf dieselbe Stelle lassen sich zusammenführen.");
        }
        if (differentAccounts(target, source)) {
            throw new BusinessRuleException("Die Bewerbungen stammen von verschiedenen Bewerber-Konten "
                    + "und können nicht zusammengeführt werden.");
        }
        validateMoves(source, request.documents());

        for (DocumentMove move : request.documents()) {
            moveDocument(source, target, move);
        }
        if (!request.documents().isEmpty()) {
            target.clearAnalysis();
        }
        copyContactData(source.getCandidate(), target.getCandidate());
        mergeNotes(source, target);
        target.markSeenByHr(LocalDateTime.now(clock));

        Optional<UserAccount> account = target.getApplicantAccount().isEmpty()
                ? source.getApplicantAccount() : Optional.empty();
        applicationRepository.delete(source);
        applicationRepository.flush();
        // Erst nach dem Löschen übernehmen: Ein Konto darf pro Stelle nur eine Bewerbung haben.
        account.ifPresent(target::setApplicantAccount);
        return ApplicationMapper.toDetail(target);
    }

    private static void validateMoves(Application source, List<DocumentMove> moves) {
        Set<DocumentType> from = new HashSet<>();
        Set<DocumentType> to = new HashSet<>();
        for (DocumentMove move : moves) {
            if (source.getDocumentFileName(move.from()).isEmpty()) {
                throw new BusinessRuleException("Die Bewerbung enthält kein Dokument „" + move.from().label() + "“.");
            }
            if (!from.add(move.from()) || !to.add(move.to())) {
                throw new BusinessRuleException("Jede Unterlage und jede Dokumentart darf nur einmal vorkommen.");
            }
        }
    }

    private static void moveDocument(Application source, Application target, DocumentMove move) {
        ResumeDocument original = source.getDocument(move.from())
                .orElseThrow(() -> new BusinessRuleException("Das Original-PDF fehlt."));
        // Kopie statt Umhängen: Die Quelle wird samt ihren Unterlagen gelöscht.
        target.setDocument(move.to(),
                new ResumeDocument(original.getFileName(), original.getContentType(), original.getData()));
        String text = textOf(source.getCandidate(), move.from());
        Candidate candidate = target.getCandidate();
        switch (move.to()) {
            case RESUME -> candidate.replaceResumeText(text);
            case SKILL_PROFILE -> candidate.replaceSkillProfileText(text);
            case COVER_LETTER -> candidate.setCoverLetterText(text);
        }
    }

    private static String textOf(Candidate candidate, DocumentType type) {
        return switch (type) {
            case RESUME -> candidate.getRawResumeText();
            case SKILL_PROFILE -> candidate.getSkillProfileText().orElse("");
            case COVER_LETTER -> candidate.getCoverLetterText().orElse("");
        };
    }

    private static void copyContactData(Candidate source, Candidate target) {
        if (target.getEmail() == null) {
            target.setEmail(source.getEmail());
        }
        if (target.getPhone() == null) {
            target.setPhone(source.getPhone());
        }
        if (target.getCity() == null) {
            target.setCity(source.getCity());
        }
    }

    private static void mergeNotes(Application source, Application target) {
        String sourceNotes = source.getInterviewNotes();
        if (sourceNotes == null || sourceNotes.isBlank()) {
            return;
        }
        String targetNotes = target.getInterviewNotes();
        target.setInterviewNotes(targetNotes == null || targetNotes.isBlank()
                ? sourceNotes : targetNotes + "\n\n" + sourceNotes);
    }

    /**
     * {@code true}, wenn beide Bewerbungen über das Portal kamen, aber von verschiedenen Konten – also sicher von
     * verschiedenen Personen. Liest nur die Konto-IDs (Fremdschlüssel), lädt die Konten nicht.
     */
    static boolean differentAccounts(Application a, Application b) {
        Optional<Long> accountA = a.getApplicantAccount().map(UserAccount::getId);
        Optional<Long> accountB = b.getApplicantAccount().map(UserAccount::getId);
        return accountA.isPresent() && accountB.isPresent() && !accountA.equals(accountB);
    }

    static Optional<GroupMatch> match(Candidate a, Candidate b) {
        if (a.getEmail() != null && a.getEmail().equalsIgnoreCase(b.getEmail())) {
            return Optional.of(GroupMatch.EMAIL);
        }
        Optional<String> phone = ResumeHeuristics.normalizePhone(a.getPhone());
        if (phone.isPresent() && phone.equals(ResumeHeuristics.normalizePhone(b.getPhone()))) {
            return Optional.of(GroupMatch.PHONE);
        }
        if (knownName(a).isPresent() && knownName(a).equals(knownName(b))) {
            return Optional.of(GroupMatch.NAME);
        }
        return Optional.empty();
    }

    private static Optional<String> knownName(Candidate candidate) {
        boolean known = candidate.getFirstName() != null && candidate.getLastName() != null
                && new ResumeHeuristics.Name(candidate.getFirstName(), candidate.getLastName()).isKnown();
        return known
                ? Optional.of((candidate.getFirstName() + " " + candidate.getLastName()).toLowerCase(Locale.GERMAN))
                : Optional.empty();
    }

    private static DuplicateDto toDuplicate(Application other, GroupMatch reason) {
        Candidate candidate = other.getCandidate();
        List<DocumentRefDto> documents = new ArrayList<>();
        for (DocumentType type : DocumentType.values()) {
            other.getDocumentFileName(type).ifPresent(fileName -> documents.add(new DocumentRefDto(type, fileName,
                    type == DocumentType.RESUME
                            ? DocumentClassifier.classify(fileName, candidate.getRawResumeText()) : type)));
        }
        return new DuplicateDto(other.getId(), candidate.getFirstName(), candidate.getLastName(),
                other.getCreatedAt(), other.getStatus(), reason, documents);
    }

    private Application loadWithDocuments(Long id) {
        return applicationRepository.findWithDocumentsById(id)
                .orElseThrow(() -> new ResourceNotFoundException(APPLICATION, id));
    }
}
