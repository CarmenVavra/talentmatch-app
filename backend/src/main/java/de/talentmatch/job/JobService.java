package de.talentmatch.job;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.talentmatch.application.Application;
import de.talentmatch.application.ApplicationRepository;
import de.talentmatch.application.ApplicationRepository.JobAverageScore;
import de.talentmatch.application.ApplicationRepository.JobStatusCount;
import de.talentmatch.application.ApplicationStatus;
import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.job.api.CreateJobRequest;
import de.talentmatch.job.api.JobDto;

@Service
public class JobService {

    private final JobPositionRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final Clock clock;

    public JobService(JobPositionRepository jobRepository, ApplicationRepository applicationRepository, Clock clock) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.clock = clock;
    }

    /**
     * Alle Stellen (neueste zuerst) inkl. Zählern – insgesamt drei Abfragen, unabhängig von der Anzahl Stellen.
     */
    @Transactional(readOnly = true)
    public List<JobDto> listJobs() {
        Map<Long, Map<ApplicationStatus, Long>> counts = loadStatusCounts();
        Map<Long, Integer> averages = loadAverageScores();
        return jobRepository.findAllByOrderByCreatedDateDescIdAsc().stream()
                .map(job -> toDto(job, counts.get(job.getId()), averages.get(job.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<JobDto> findJob(Long id) {
        return jobRepository.findById(id).map(job -> toDto(job,
                loadStatusCounts().get(job.getId()), loadAverageScores().get(job.getId())));
    }

    @Transactional
    public JobDto createJob(CreateJobRequest request) {
        JobPosition job = new JobPosition(request.title().trim(), request.department().trim(),
                request.experienceLevel().trim(), request.location().trim(), request.description(),
                normalizeSkills(request.requiredSkills()), LocalDate.now(clock));
        return toDto(jobRepository.save(job), null, null);
    }

    /**
     * Ändert eine Stelle. Bestehende KI-Analysen bleiben unverändert (sie lassen sich in der Detailansicht neu
     * starten).
     */
    @Transactional
    public JobDto updateJob(Long id, CreateJobRequest request) {
        JobPosition job = jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Stelle", id));
        job.update(request.title().trim(), request.department().trim(), request.experienceLevel().trim(),
                request.location().trim(), request.description(), normalizeSkills(request.requiredSkills()));
        return toDto(job, loadStatusCounts().get(id), loadAverageScores().get(id));
    }

    /**
     * Löscht eine Stelle endgültig – inklusive aller Bewerbungen, Unterlagen und KI-Analysen.
     *
     * @return Anzahl der mitgelöschten Bewerbungen
     */
    @Transactional
    public int deleteJob(Long id) {
        JobPosition job = jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Stelle", id));
        List<Application> applications = applicationRepository.findAllWithDependenciesByJobPositionId(id);
        applicationRepository.deleteAll(applications);
        jobRepository.delete(job);
        return applications.size();
    }

    private static List<String> normalizeSkills(List<String> skills) {
        return skills.stream()
                .map(String::trim)
                .filter(skill -> !skill.isEmpty())
                .distinct()
                .toList();
    }

    private Map<Long, Map<ApplicationStatus, Long>> loadStatusCounts() {
        Map<Long, Map<ApplicationStatus, Long>> result = new HashMap<>();
        for (JobStatusCount row : applicationRepository.countByJobAndStatus()) {
            result.computeIfAbsent(row.getJobId(), id -> new EnumMap<>(ApplicationStatus.class))
                    .put(row.getStatus(), row.getTotal());
        }
        return result;
    }

    private Map<Long, Integer> loadAverageScores() {
        Map<Long, Integer> result = new HashMap<>();
        for (JobAverageScore row : applicationRepository.averageScoreByJob()) {
            if (row.getAverageScore() != null) {
                result.put(row.getJobId(), (int) Math.round(row.getAverageScore()));
            }
        }
        return result;
    }

    private static JobDto toDto(JobPosition job, Map<ApplicationStatus, Long> counts, Integer averageScore) {
        Map<ApplicationStatus, Long> statusCounts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            statusCounts.put(status, counts == null ? 0L : counts.getOrDefault(status, 0L));
        }
        long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        return new JobDto(job.getId(), job.getTitle(), job.getDepartment(), job.getExperienceLevel(),
                job.getLocation(), job.getDescription(), job.getRequiredSkills(), job.getCreatedDate(),
                statusCounts, total, averageScore);
    }
}
