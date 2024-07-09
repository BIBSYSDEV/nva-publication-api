package no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class LiteraryArtsPerformanceSubtype {

    public static final String TYPE_FIELD = "type";
    public static final String DESCRIPTION_FIELD = "description";
    @JsonProperty(TYPE_FIELD)
    private final LiteraryArtsPerformanceSubtypeEnum type;

    public static LiteraryArtsPerformanceSubtype createOther(String description) {
        return new LiteraryArtsPerformanceSubtypeOther(LiteraryArtsPerformanceSubtypeEnum.OTHER, description);
    }

    @JacocoGenerated
    @JsonCreator
    public static LiteraryArtsPerformanceSubtype create(Object subtype) {
        if (subtype instanceof String) {
            return create(LiteraryArtsPerformanceSubtypeEnum.parse((String) subtype));
        } else if (subtype instanceof LiteraryArtsPerformanceSubtypeEnum) {
            return create((LiteraryArtsPerformanceSubtypeEnum) subtype);
        } else if (subtype instanceof Map) {
            return parseCurrentImplementation((LinkedHashMap<String, String>) subtype);
        } else {
            throw new IllegalArgumentException("Invalid subtype");
        }
    }

    public static LiteraryArtsPerformanceSubtype create(LiteraryArtsPerformanceSubtypeEnum type) {
        return new LiteraryArtsPerformanceSubtype(type);
    }

    private static LiteraryArtsPerformanceSubtype parseCurrentImplementation(Map<String, String> subtype) {
        var type = LiteraryArtsPerformanceSubtypeEnum.parse(subtype.get(TYPE_FIELD));
        return fromJson(type, subtype.get(DESCRIPTION_FIELD));
    }

    @JacocoGenerated
    public static LiteraryArtsPerformanceSubtype fromJson(
            @JsonProperty(TYPE_FIELD) LiteraryArtsPerformanceSubtypeEnum type,
            @JsonProperty(DESCRIPTION_FIELD) String description) {
        if (LiteraryArtsPerformanceSubtypeEnum.OTHER.equals(type)) {
            return createOther(description);
        }
        return new LiteraryArtsPerformanceSubtype(type);
    }

    protected LiteraryArtsPerformanceSubtype(LiteraryArtsPerformanceSubtypeEnum type) {
        this.type = type;
    }

    public LiteraryArtsPerformanceSubtypeEnum getType() {
        return type;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LiteraryArtsPerformanceSubtype)) {
            return false;
        }
        LiteraryArtsPerformanceSubtype that = (LiteraryArtsPerformanceSubtype) o;
        return getType() == that.getType();
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getType());
    }
}
