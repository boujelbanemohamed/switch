package com.switchplatform.platform.iso8583;

import com.solab.iso8583.CustomField;

public class ReservedPrivateField implements CustomField<String> {

    @Override
    public String decodeField(String value) {
        return value;
    }

    @Override
    public String encodeField(String value) {
        return value;
    }

}
