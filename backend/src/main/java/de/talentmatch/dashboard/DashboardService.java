package de.talentmatch.dashboard;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.talentmatch.application.ApplicationRepository;
import de.talentmatch.application.ApplicationStatus;
import de.talentmatch.job.JobPositionRepository;

@Service
public class DashboardService {

    private final JobPositionRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final Clock clock;

    public DashboardService(JobPositionRepository jobRepository, ApplicationRepository applicationRepository,
                            Clock clock) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardKpisDto kpis() {
        LocalDate today = LocalDate.now(clock);
        return new DashboardKpisDto(
                jobRepository.count(),
                jobRepository.countByCreatedDateGreaterThanEqual(today.withDayOfMonth(1)),
                applicationRepository.count(),
                applicationRepository.countByCreatedAtGreaterThanEqual(LocalDateTime.now(clock).minusDays(7)),
                applicationRepository.countByStatus(ApplicationStatus.NEW),
                applicationRepository.countByStatus(ApplicationStatus.INTERVIEW));
    }
}
