package de.talentmatch.job.api;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateJobRequest(
        @NotBlank(message = "Titel ist erforderlich.")
        @Size(max = 200, message = "Der Titel darf höchstens 200 Zeichen lang sein.")
        String title,
        @NotBlank(message = "Abteilung ist erforderlich.")
        String department,
        @NotBlank(message = "Level ist erforderlich.")
        String experienceLevel,
        @NotBlank(message = "Ort ist erforderlich.")
        String location,
        @Size(max = 4000, message = "Die Beschreibung darf höchstens 4000 Zeichen lang sein.")
        String description,
        @NotEmpty(message = "Mindestens ein Skill ist erforderlich.")
        @Size(max = 20, message = "Höchstens 20 Skills.")
        List<@NotBlank String> requiredSkills) {
}
