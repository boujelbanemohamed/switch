package com.switchplatform.platform.model.authorization;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ActionTypeConverter implements AttributeConverter<AuthRule.ActionType, String> {

    @Override
    public String convertToDatabaseColumn(AuthRule.ActionType attribute) {
        if (attribute == AuthRule.ActionType.TWO_FA) return "2FA";
        return attribute.name();
    }

    @Override
    public AuthRule.ActionType convertToEntityAttribute(String dbData) {
        if ("2FA".equals(dbData)) return AuthRule.ActionType.TWO_FA;
        return AuthRule.ActionType.valueOf(dbData);
    }
}
