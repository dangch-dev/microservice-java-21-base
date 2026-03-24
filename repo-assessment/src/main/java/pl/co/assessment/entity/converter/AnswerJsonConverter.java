package pl.co.assessment.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pl.co.assessment.entity.json.AnswerJson;

@Converter
public class AnswerJsonConverter implements AttributeConverter<AnswerJson, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(AnswerJson attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to serialize answer json", ex);
        }
    }

    @Override
    public AnswerJson convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, AnswerJson.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to deserialize answer json", ex);
        }
    }
}
