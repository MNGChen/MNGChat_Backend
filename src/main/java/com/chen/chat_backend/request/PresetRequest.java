package com.chen.chat_backend.request;

import lombok.Data;

@Data
// Payload used when a user saves the current chat settings as a preset.
public class PresetRequest {
    private String name;
    private String systemPrompt;
    private Double temperature;
}
