package pl.co.assessment.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pl.co.assessment.entity.json.GroupPromptContent;

@Converter
public class GroupPromptContentConverter implements AttributeConverter<GroupPromptContent, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int SCHEMA_VERSION = 1;

    @Override
    public String convertToDatabaseColumn(GroupPromptContent attribute) {
        if (attribute == null) {
            return null;
        }
        attribute.setSchemaVersion(SCHEMA_VERSION);
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to serialize group prompt content", ex);
        }
    }

    @Override
    public GroupPromptContent convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, GroupPromptContent.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to deserialize group prompt content", ex);
        }
    }
}
