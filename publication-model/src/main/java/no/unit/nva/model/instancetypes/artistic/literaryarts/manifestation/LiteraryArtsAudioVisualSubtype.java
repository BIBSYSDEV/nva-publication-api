package no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class LiteraryArtsAudioVisualSubtype {

    public static final String TYPE_FIELD = "type";
    public static final String DESCRIPTION_FIELD = "description";
    @JsonProperty(TYPE_FIELD)
    private final LiteraryArtsAudioVisualSubtypeEnum type;

    public static LiteraryArtsAudioVisualSubtype createOther(String description) {
        return new LiteraryArtsAudioVisualSubtypeOther(LiteraryArtsAudioVisualSubtypeEnum.OTHER, description);
    }

    @JacocoGenerated
    @JsonCreator
    public static LiteraryArtsAudioVisualSubtype create(Object subtype) {
        if (subtype instanceof String) {
            return new LiteraryArtsAudioVisualSubtype(LiteraryArtsAudioVisualSubtypeEnum.parse((String) subtype));
        } else if (subtype instanceof LiteraryArtsAudioVisualSubtypeEnum) {
            return new LiteraryArtsAudioVisualSubtype((LiteraryArtsAudioVisualSubtypeEnum) subtype);
        } else if (subtype instanceof Map) {
            return parseCurrentImplementation((LinkedHashMap<String, String>) subtype);
        } else {
            throw new IllegalArgumentException("Invalid subtype");
        }
    }

    public static LiteraryArtsAudioVisualSubtype create(LiteraryArtsAudioVisualSubtypeEnum type) {
        return new LiteraryArtsAudioVisualSubtype(type);
    }

    private static LiteraryArtsAudioVisualSubtype parseCurrentImplementation(Map<String, String> subtype) {
        var type = LiteraryArtsAudioVisualSubtypeEnum.parse(subtype.get(TYPE_FIELD));
        return fromJson(type, subtype.get(DESCRIPTION_FIELD));
    }

    @JacocoGenerated
    public static LiteraryArtsAudioVisualSubtype fromJson(
            @JsonProperty(TYPE_FIELD) LiteraryArtsAudioVisualSubtypeEnum type,
            @JsonProperty(DESCRIPTION_FIELD) String description) {
        if (LiteraryArtsAudioVisualSubtypeEnum.OTHER.equals(type)) {
            return createOther(description);
        }
        return new LiteraryArtsAudioVisualSubtype(type);
    }

    protected LiteraryArtsAudioVisualSubtype(LiteraryArtsAudioVisualSubtypeEnum type) {
        this.type = type;
    }

    public LiteraryArtsAudioVisualSubtypeEnum getType() {
        return type;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LiteraryArtsAudioVisualSubtype)) {
            return false;
        }
        LiteraryArtsAudioVisualSubtype that = (LiteraryArtsAudioVisualSubtype) o;
        return getType() == that.getType();
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getType());
    }
}
