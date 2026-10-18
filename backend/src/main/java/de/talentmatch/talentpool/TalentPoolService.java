package de.talentmatch.talentpool;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.talentmatch.application.Application;
import de.talentmatch.application.ApplicationDeletionService;
import de.talentmatch.application.ApplicationMapper;
import de.talentmatch.application.ApplicationRepository;
import de.talentmatch.application.api.ApplicationDetailDto;
import de.talentmatch.auth.UserAccount;
import de.talentmatch.auth.UserAccountRepository;
import de.talentmatch.candidate.Candidate;
import de.talentmatch.common.exception.BusinessRuleException;
import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.config.TalentMatchProperties;
import de.talentmatch.mail.MailService;
import de.talentmatch.talentpool.api.PublicTalentPoolDto;
import de.talentmatch.talentpool.api.TalentPoolEntryDto;
import de.talentmatch.talentpool.api.TalentPoolMailDto;
import de.talentmatch.talentpool.api.TalentPoolRequest;

/**
 * Evidenz: Absage mit Bitte um Einwilligung, Antwort der Bewerber:in (ohne Anmeldung über einen persönlichen
 * Link), Erinnerung und automatisches Löschen ohne Antwort bzw. nach Ablauf der Aufbewahrungsdauer.
 */
@Service
public class TalentPoolService {

    private static final String APPLICATION = "Bewerbung";
    private static final String INVALID_LINK = "Dieser Link ist nicht mehr gültig.";
    private static final int TOKEN_BYTES = 32;

    private final ApplicationRepository applicationRepository;
    private final ApplicationDeletionService deletionService;
    private final UserAccountRepository userRepository;
    private final MailService mailService;
    private final TalentMatchProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public TalentPoolService(ApplicationRepository applicationRepository, ApplicationDeletionService deletionService,
                             UserAccountRepository userRepository, MailService mailService,
                             TalentMatchProperties properties, Clock clock) {
        this.applicationRepository = applicationRepository;
        this.deletionService = deletionService;
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Vorschlag für die Anfrage-E-Mail, unterschrieben mit Name und Abteilung der angemeldeten HR-Person.
     */
    @Transactional(readOnly = true)
    public TalentPoolMailDto preview(Long applicationId, String hrEmail) {
        Application application = load(applicationId);
        Candidate candidate = application.getCandidate();
        String recipient = requireEmail(candidate);
        String jobTitle = application.getJobPosition().getTitle();
        return new TalentPoolMailDto(recipient, TalentPoolMails.requestSubject(jobTitle),
                TalentPoolMails.requestBody(candidate.getFirstName(), candidate.getLastName(), jobTitle,
                        signature(hrEmail)), mailService.deliveryEnabled());
    }

    /**
     * Sagt ab, fragt die Einwilligung zur Evidenz an und legt die E-Mail (mit persönlichen Links) in den
     * Postausgang.
     */
    @Transactional
    public ApplicationDetailDto request(Long applicationId, TalentPoolRequest request) {
        Application application = load(applicationId);
        String recipient = requireEmail(application.getCandidate());
        if (application.getTalentPool().isPresent()) {
            throw new BusinessRuleException("Für diese Bewerbung wurde die Evidenz bereits angefragt.");
        }
        LocalDateTime now = now();
        LocalDateTime deadline = now.plus(properties.talentPool().responsePeriod());
        String token = newToken();
        application.requestTalentPool(TalentPoolConsent.request(token, now, deadline));
        application.markSeenByHr(now);
        String body = TalentPoolMails.withLinks(request.body().strip(), answerUrl(token), deadline,
                properties.talentPool().retention().toDays() / 30);
        mailService.queue(recipient, request.subject().strip(), body, application.getId());
        return ApplicationMapper.toDetail(application);
    }

    /**
     * Antwortseite: nur gültige Anfragen bzw. Einwilligungen liefern Daten.
     */
    @Transactional(readOnly = true)
    public Optional<PublicTalentPoolDto> findByToken(String token) {
        return applicationRepository.findByTalentPoolToken(token).flatMap(application ->
                application.getTalentPool().map(consent -> new PublicTalentPoolDto(
                        application.getCandidate().getFirstName(), application.getJobPosition().getTitle(),
                        consent.getStatus(), consent.getResponseDeadline(), consent.getExpiresAt())));
    }

    /**
     * Zustimmung der Bewerber:in. Mehrfaches Bestätigen ist unschädlich.
     */
    @Transactional
    public PublicTalentPoolDto confirm(String token) {
        Application application = loadByToken(token);
        TalentPoolConsent consent = application.getTalentPool().orElseThrow();
        if (consent.getStatus() == TalentPoolStatus.REQUESTED) {
            LocalDateTime now = now();
            consent.confirm(now, now.plus(properties.talentPool().retention()));
            Candidate candidate = application.getCandidate();
            mailService.queue(candidate.getEmail(), TalentPoolMails.confirmationSubject(),
                    TalentPoolMails.confirmationBody(candidate.getFirstName(), candidate.getLastName(),
                            answerUrl(token), consent.getExpiresAt()), application.getId());
        }
        return findByToken(token).orElseThrow();
    }

    /**
     * „Nein, danke“ bzw. Widerruf: Die Bewerbung wird sofort vollständig gelöscht.
     */
    @Transactional
    public void decline(String token) {
        deletionService.delete(loadByToken(token));
    }

    @Transactional(readOnly = true)
    public List<TalentPoolEntryDto> list() {
        return applicationRepository.findInTalentPool().stream()
                .map(application -> new TalentPoolEntryDto(application.getId(),
                        application.getCandidate().getFirstName(), application.getCandidate().getLastName(),
                        application.getCandidate().getEmail(), application.getJobPosition().getId(),
                        application.getJobPosition().getTitle(),
                        application.getTalentPool().map(ApplicationMapper::toTalentPool).orElse(null)))
                .toList();
    }

    /**
     * Regelmäßige Pflege: Erinnerung nach {@code reminderAfter}, Löschen ohne Antwort nach Fristende und nach
     * Ablauf der Aufbewahrungsdauer.
     */
    @Transactional
    public MaintenanceResult runMaintenance(LocalDateTime now) {
        List<Application> toRemind = applicationRepository.findTalentPoolRequestsToRemind(
                now.minus(properties.talentPool().reminderAfter()), now);
        for (Application application : toRemind) {
            TalentPoolConsent consent = application.getTalentPool().orElseThrow();
            Candidate candidate = application.getCandidate();
            mailService.queue(candidate.getEmail(),
                    TalentPoolMails.reminderSubject(application.getJobPosition().getTitle()),
                    TalentPoolMails.reminderBody(candidate.getFirstName(), candidate.getLastName(),
                            application.getJobPosition().getTitle(), answerUrl(consent.getToken()),
                            consent.getResponseDeadline()), application.getId());
            consent.markReminded(now);
        }
        List<Application> unanswered = applicationRepository.findExpiredTalentPoolRequests(now);
        List<Application> expired = applicationRepository.findExpiredTalentPoolConsents(now);
        deletionService.deleteAll(unanswered);
        deletionService.deleteAll(expired);
        return new MaintenanceResult(toRemind.size(), unanswered.size(), expired.size());
    }

    private String signature(String hrEmail) {
        return userRepository.findByEmailIgnoreCase(hrEmail)
                .map(TalentPoolService::signatureOf)
                .orElse("Ihr Recruiting-Team");
    }

    private static String signatureOf(UserAccount account) {
        String name = account.getFirstName() + " " + account.getLastName();
        return account.getDepartment() == null || account.getDepartment().isBlank()
                ? name : name + "\n" + account.getDepartment();
    }

    private static String requireEmail(Candidate candidate) {
        if (candidate.getEmail() == null || candidate.getEmail().isBlank()) {
            throw new BusinessRuleException(
                    "Für diese Bewerbung ist keine E-Mail-Adresse bekannt – die Evidenz kann nicht angefragt werden.");
        }
        return candidate.getEmail();
    }

    private String answerUrl(String token) {
        String base = properties.mail().appBaseUrl().replaceAll("/+$", "");
        return base + "/evidenz/" + token;
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Application load(Long id) {
        return applicationRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException(APPLICATION, id));
    }

    private Application loadByToken(String token) {
        return applicationRepository.findByTalentPoolToken(token)
                .filter(application -> application.getTalentPool().isPresent())
                .orElseThrow(() -> new BusinessRuleException(INVALID_LINK));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * Ergebnis eines Pflegelaufs.
     */
    public record MaintenanceResult(int reminded, int deletedUnanswered, int deletedExpired) {
    }
}
