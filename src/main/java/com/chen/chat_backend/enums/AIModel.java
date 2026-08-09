package com.chen.chat_backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

// Models the frontend may select; each value maps to the provider API name.
public enum AIModel {
    GPT_5_4("gpt-5.4"),
    GPT_5_4_MINI("gpt-5.4-mini");

    private final String apiName;

    AIModel(String s) {
        this.apiName = s;
    }

    @JsonValue
    public String getApiName() {
        return apiName;
    }

    @JsonCreator
    public static AIModel fromValue(String value) {
        if (value == null || value.isBlank()) {
            return defaultModel();
        }

        for (AIModel model : values()) {
            if (model.name().equalsIgnoreCase(value) || model.apiName.equalsIgnoreCase(value)) {
                return model;
            }
        }

        throw new IllegalArgumentException("Unsupported AI model: " + value);
    }

    public static AIModel defaultModel() {
        return GPT_5_4;
    }
}
