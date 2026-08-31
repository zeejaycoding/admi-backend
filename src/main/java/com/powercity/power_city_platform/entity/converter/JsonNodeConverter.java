package com.powercity.power_city_platform.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class JsonNodeConverter implements AttributeConverter<JsonNode, Object> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Object convertToDatabaseColumn(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }
        try {
            // Return the JSON string which PostgreSQL will cast to JSONB
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JsonNode to JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public JsonNode convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            // Handle both String and PGobject from PostgreSQL
            String jsonString;
            if (dbData instanceof String) {
                jsonString = (String) dbData;
            } else {
                jsonString = dbData.toString();
            }

            if (jsonString.isEmpty()) {
                return null;
            }

            return objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON to JsonNode: " + e.getMessage(), e);
        }
    }
}
