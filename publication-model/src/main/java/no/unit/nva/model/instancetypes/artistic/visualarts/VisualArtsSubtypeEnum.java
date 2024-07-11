package no.unit.nva.model.instancetypes.artistic.visualarts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;

import java.util.Arrays;

public enum VisualArtsSubtypeEnum {
    INDIVIDUAL_EXHIBITION("IndividualExhibition"),
    COLLECTIVE_EXHIBITION("CollectiveExhibition"),
    INSTALLATION("Installation"),
    ART_IN_PUBLIC_SPACE("ArtInPublicSpace"),
    PERFORMANCE("Performance"),
    VISUAL_ARTS("VisualArts"),
    AUDIO_ART("AudioArt"),
    ARTIST_BOOK("ArtistBook"),
    OTHER("VisualArtsOther");

    private final String type;

    VisualArtsSubtypeEnum(String type) {
        this.type = type;
    }

    @JsonValue
    public String getType() {
        return type;
    }

    // TODO: Remove following migration
    @Deprecated
    @JsonCreator
    public static VisualArtsSubtypeEnum parse(String candidate) {
        return "Other".equalsIgnoreCase(candidate)
                ? VisualArtsSubtypeEnum.OTHER
                : inlineableParseMethod(candidate);
    }

    public static VisualArtsSubtypeEnum inlineableParseMethod(String candidate) {
        return Arrays.stream(VisualArtsSubtypeEnum.values())
                .filter(value -> value.getType().equals(candidate))
                .collect(SingletonCollector.collect());
    }
}
