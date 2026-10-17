package de.talentmatch.job;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPositionRepository extends JpaRepository<JobPosition, Long> {

    List<JobPosition> findAllByOrderByCreatedDateDescIdAsc();

    long countByCreatedDateGreaterThanEqual(LocalDate date);
}
