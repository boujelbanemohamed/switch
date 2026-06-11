package com.switchplatform.platform.model.fraud;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class FraudActionConverter implements AttributeConverter<FraudRule.Action, String> {

    @Override
    public String convertToDatabaseColumn(FraudRule.Action attribute) {
        if (attribute == FraudRule.Action.TWO_FA) return "2FA";
        return attribute.name();
    }

    @Override
    public FraudRule.Action convertToEntityAttribute(String dbData) {
        if ("2FA".equals(dbData)) return FraudRule.Action.TWO_FA;
        return FraudRule.Action.valueOf(dbData);
    }
}
