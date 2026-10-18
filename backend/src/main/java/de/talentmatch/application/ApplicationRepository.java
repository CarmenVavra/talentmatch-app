package de.talentmatch.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Bewerbungen. Listenabfragen laden Kandidat:in und Analyse per {@link EntityGraph} mit (kein N+1).
 */
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    @EntityGraph(attributePaths = {"candidate", "analysis"})
    List<Application> findByJobPositionIdOrderByCreatedAtDesc(Long jobPositionId);

    @EntityGraph(attributePaths = {"candidate", "analysis", "jobPosition"})
    List<Application> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"candidate", "analysis", "jobPosition"})
    Optional<Application> findWithDetailsById(Long id);

    /**
     * Bewerbungen einer Stelle samt allen abhängigen Daten – zum Löschen in einem Rutsch (keine Nachlade-Abfragen).
     */
    @EntityGraph(attributePaths = {"candidate", "analysis", "resumeDocument", "skillProfileDocument",
        "coverLetterDocument"})
    List<Application> findAllWithDependenciesByJobPositionId(Long jobPositionId);

    /**
     * Bewerbung mit allen Unterlagen – zum Zusammenführen.
     */
    @EntityGraph(attributePaths = {"candidate", "analysis", "jobPosition", "resumeDocument", "skillProfileDocument",
        "coverLetterDocument"})
    Optional<Application> findWithDocumentsById(Long id);

    /**
     * Alle Bewerbungen in Evidenz (angefragt oder bestätigt), neueste Anfrage zuerst.
     */
    @EntityGraph(attributePaths = {"candidate", "jobPosition"})
    @Query("select a from Application a where a.talentPool.status is not null order by a.talentPool.requestedAt desc")
    List<Application> findInTalentPool();

    @EntityGraph(attributePaths = {"candidate", "jobPosition"})
    @Query("select a from Application a where a.talentPool.token = :token")
    Optional<Application> findByTalentPoolToken(@Param("token") String token);

    /**
     * Unbeantwortete Anfragen, deren Frist abgelaufen ist.
     */
    @Query("""
            select a from Application a
            where a.talentPool.status = de.talentmatch.talentpool.TalentPoolStatus.REQUESTED
              and a.talentPool.responseDeadline < :now""")
    List<Application> findExpiredTalentPoolRequests(@Param("now") LocalDateTime now);

    /**
     * Unbeantwortete Anfragen, die eine Erinnerung brauchen (angefragt vor {@code remindBefore}, Frist läuft noch).
     */
    @EntityGraph(attributePaths = {"candidate", "jobPosition"})
    @Query("""
            select a from Application a
            where a.talentPool.status = de.talentmatch.talentpool.TalentPoolStatus.REQUESTED
              and a.talentPool.reminderSentAt is null
              and a.talentPool.requestedAt <= :remindBefore
              and a.talentPool.responseDeadline >= :now""")
    List<Application> findTalentPoolRequestsToRemind(@Param("remindBefore") LocalDateTime remindBefore,
                                                     @Param("now") LocalDateTime now);

    /**
     * Bestätigte Evidenz, deren Aufbewahrungsdauer abgelaufen ist.
     */
    @Query("""
            select a from Application a
            where a.talentPool.status = de.talentmatch.talentpool.TalentPoolStatus.CONFIRMED
              and a.talentPool.expiresAt < :now""")
    List<Application> findExpiredTalentPoolConsents(@Param("now") LocalDateTime now);

    /**
     * Absagen ohne erfassten Fristbeginn (z. B. aus älteren Datenbeständen) beginnen ihre Frist jetzt.
     */
    @Modifying
    @Query("""
            update Application a set a.rejectedAt = :now
            where a.status = de.talentmatch.application.ApplicationStatus.REJECTED and a.rejectedAt is null""")
    int startMissingRejectionPeriods(@Param("now") LocalDateTime now);

    /**
     * Absagen ohne Evidenz, deren Aufbewahrungsfrist vor {@code cutoff} begonnen hat.
     */
    @Query("""
            select a from Application a
            where a.status = de.talentmatch.application.ApplicationStatus.REJECTED
              and a.talentPool.status is null
              and a.rejectedAt < :cutoff""")
    List<Application> findRejectedBefore(@Param("cutoff") LocalDateTime cutoff);

    @EntityGraph(attributePaths = {"resumeDocument"})
    Optional<Application> findWithResumeById(Long id);

    @Query("""
            select a.jobPosition.id as jobId, a.status as status, count(a) as total
            from Application a
            group by a.jobPosition.id, a.status
            """)
    List<JobStatusCount> countByJobAndStatus();

    @Query("""
            select a.jobPosition.id as jobId, avg(r.overallScore) as averageScore
            from Application a join a.analysis r
            group by a.jobPosition.id
            """)
    List<JobAverageScore> averageScoreByJob();

    /** Eigene Bewerbungen einer Bewerber:in (Portal), neueste zuerst. */
    @EntityGraph(attributePaths = {"jobPosition"})
    List<Application> findByApplicantAccountIdOrderByCreatedAtDesc(Long accountId);

    boolean existsByJobPositionIdAndApplicantAccountId(Long jobPositionId, Long accountId);

    @Query("select a.jobPosition.id from Application a where a.applicantAccount.id = :accountId")
    List<Long> findJobIdsByApplicantAccountId(@Param("accountId") Long accountId);

    long countByStatus(ApplicationStatus status);

    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    /**
     * Projektion: Anzahl Bewerbungen je Stelle und Status.
     */
    interface JobStatusCount {
        Long getJobId();

        ApplicationStatus getStatus();

        long getTotal();
    }

    /**
     * Projektion: durchschnittlicher Match-Score je Stelle.
     */
    interface JobAverageScore {
        Long getJobId();

        Double getAverageScore();
    }
}
