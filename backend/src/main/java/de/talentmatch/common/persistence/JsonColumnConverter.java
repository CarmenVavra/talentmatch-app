package de.talentmatch.common.persistence;

import jakarta.persistence.AttributeConverter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Basisklasse für JPA-Konverter, die Werte als JSON-Text in einer Spalte ablegen.
 * Funktioniert identisch auf H2 und PostgreSQL und vermeidet Element-Collections (keine N+1-Abfragen).
 *
 * @param <T> Java-Typ des Attributs
 */
public abstract class JsonColumnConverter<T> implements AttributeConverter<T, String> {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final TypeReference<T> type;

    protected JsonColumnConverter(TypeReference<T> type) {
        this.type = type;
    }

    /**
     * Wert, der bei leerer Spalte zurückgegeben wird.
     */
    protected abstract T emptyValue();

    @Override
    public String convertToDatabaseColumn(T attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JacksonException ex) {
            throw new IllegalStateException("JSON-Serialisierung fehlgeschlagen", ex);
        }
    }

    @Override
    public T convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return emptyValue();
        }
        try {
            return MAPPER.readValue(dbData, type);
        } catch (JacksonException ex) {
            throw new IllegalStateException("JSON-Deserialisierung fehlgeschlagen", ex);
        }
    }
}
