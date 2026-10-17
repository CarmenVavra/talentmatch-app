package de.talentmatch.common.persistence;

import java.util.List;

import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;

/**
 * Speichert {@code List<String>} als JSON-Array.
 */
@Converter
public class StringListConverter extends JsonColumnConverter<List<String>> {

    public StringListConverter() {
        super(new TypeReference<>() { });
    }

    @Override
    protected List<String> emptyValue() {
        return List.of();
    }
}
