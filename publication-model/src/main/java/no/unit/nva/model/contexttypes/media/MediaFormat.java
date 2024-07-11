package no.unit.nva.model.contexttypes.media;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MediaFormat {
    TEXT("Text"),
    SOUND("Sound"),
    VIDEO("Video");

    private String value;

    MediaFormat(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
