package de.talentmatch.ai;

import java.util.List;

/**
 * Structured Output: aus dem Skill-Profil extrahierte Skills.
 */
public record SkillList(List<String> skills) {

    public SkillList {
        skills = skills == null ? List.of() : List.copyOf(skills);
    }
}
