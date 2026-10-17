package de.talentmatch.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
