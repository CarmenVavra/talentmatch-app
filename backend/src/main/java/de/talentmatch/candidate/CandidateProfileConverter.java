package de.talentmatch.candidate;

import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;

import de.talentmatch.common.persistence.JsonColumnConverter;

/**
 * Speichert das extrahierte {@link CandidateProfile} als JSON (Feld {@code parsedProfile}).
 */
@Converter
public class CandidateProfileConverter extends JsonColumnConverter<CandidateProfile> {

    public CandidateProfileConverter() {
        super(new TypeReference<>() { });
    }

    @Override
    protected CandidateProfile emptyValue() {
        return null;
    }
}
