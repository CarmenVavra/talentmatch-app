package de.talentmatch.job;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.talentmatch.common.exception.ResourceNotFoundException;
import de.talentmatch.job.api.CreateJobRequest;
import de.talentmatch.job.api.JobDto;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public List<JobDto> listJobs() {
        return jobService.listJobs();
    }

    @GetMapping("/{id}")
    public JobDto getJob(@PathVariable Long id) {
        return jobService.findJob(id).orElseThrow(() -> new ResourceNotFoundException("Stelle", id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobDto createJob(@Valid @RequestBody CreateJobRequest request) {
        return jobService.createJob(request);
    }

    @PutMapping("/{id}")
    public JobDto updateJob(@PathVariable Long id, @Valid @RequestBody CreateJobRequest request) {
        return jobService.updateJob(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
    }
}
