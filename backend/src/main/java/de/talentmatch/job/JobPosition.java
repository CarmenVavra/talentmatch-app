package de.talentmatch.job;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import de.talentmatch.common.persistence.StringListConverter;

/**
 * Stellenanzeige (Vakanz) mit Anforderungsprofil.
 */
@Entity
@Table(name = "job_position")
public class JobPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String experienceLevel;

    @Column(nullable = false)
    private String location;

    @Column(length = 4000)
    private String description;

    @Convert(converter = StringListConverter.class)
    @Column(length = 4000)
    private List<String> requiredSkills = new ArrayList<>();

    @Column(nullable = false)
    private LocalDate createdDate;

    protected JobPosition() {
    }

    public JobPosition(String title, String department, String experienceLevel, String location,
                       String description, List<String> requiredSkills, LocalDate createdDate) {
        this.title = title;
        this.department = department;
        this.experienceLevel = experienceLevel;
        this.location = location;
        this.description = description;
        this.requiredSkills = new ArrayList<>(requiredSkills);
        this.createdDate = createdDate;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDepartment() {
        return department;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getRequiredSkills() {
        return List.copyOf(requiredSkills);
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }
}
