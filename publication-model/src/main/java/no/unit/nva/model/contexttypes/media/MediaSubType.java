package no.unit.nva.model.contexttypes.media;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class MediaSubType {
    
    public static final String TYPE = "type";
    @JsonProperty(TYPE)
    private final MediaSubTypeEnum type;
    
    public static MediaSubType createOther(String description) {
        return new MediaSubTypeOther(MediaSubTypeEnum.OTHER, description);
    }
    
    @JsonCreator
    public static MediaSubType fromJson(@JsonProperty(TYPE) MediaSubTypeEnum type,
                                        @JsonProperty("description") String description) {
        if (MediaSubTypeEnum.OTHER.equals(type)) {
            return createOther(description);
        }
        return new MediaSubType(type);
    }
    
    @JsonCreator
    public static MediaSubType fromJsonString(String type) {
        return MediaSubType.create(MediaSubTypeEnum.parse(type));
    }
    
    public static MediaSubType create(MediaSubTypeEnum type) {
        return new MediaSubType(type);
    }
    
    protected MediaSubType(MediaSubTypeEnum type) {
        this.type = type;
    }
    
    public MediaSubTypeEnum getType() {
        return type;
    }
    
    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MediaSubType)) {
            return false;
        }
        MediaSubType that = (MediaSubType) o;
        return getType() == that.getType();
    }
    
    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getType());
    }
}
