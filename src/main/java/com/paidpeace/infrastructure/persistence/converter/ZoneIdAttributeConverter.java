package com.paidpeace.infrastructure.persistence.converter;

import java.time.ZoneId;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ZoneIdAttributeConverter implements AttributeConverter<ZoneId, String> {

    @Override
    public String convertToDatabaseColumn(ZoneId attribute) {
        return attribute == null ? null : attribute.getId();
    }

    @Override
    public ZoneId convertToEntityAttribute(String dbData) {
        return dbData == null || dbData.isBlank() ? null : ZoneId.of(dbData.trim());
    }
}

