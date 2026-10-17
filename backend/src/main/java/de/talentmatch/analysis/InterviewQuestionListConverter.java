package de.talentmatch.analysis;

import java.util.List;

import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;

import de.talentmatch.common.persistence.JsonColumnConverter;

@Converter
public class InterviewQuestionListConverter extends JsonColumnConverter<List<InterviewQuestion>> {

    public InterviewQuestionListConverter() {
        super(new TypeReference<>() { });
    }

    @Override
    protected List<InterviewQuestion> emptyValue() {
        return List.of();
    }
}
