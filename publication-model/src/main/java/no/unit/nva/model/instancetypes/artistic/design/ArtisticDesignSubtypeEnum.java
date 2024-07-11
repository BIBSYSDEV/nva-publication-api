package no.unit.nva.model.instancetypes.artistic.design;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;

import java.util.Arrays;

public enum ArtisticDesignSubtypeEnum {
    CLOTHING_DESIGN("ClothingDesign"),
    EXHIBITION("Exhibition"),
    GRAPHIC_DESIGN("GraphicDesign"),
    ILLUSTRATION("Illustration"),
    INTERACTION_DESIGN("InteractionDesign"),
    INTERIOR_DESIGN("InteriorDesign"),
    LIGHT_DESIGN("LightDesign"),
    OTHER("ArtisticDesignOther"),
    PRODUCT_DESIGN("ProductDesign"),
    SERVICE_DESIGN("ServiceDesign"),
    WEB_DESIGN("WebDesign");

    @JsonValue
    private final String type;

    ArtisticDesignSubtypeEnum(String type) {
        this.type = type;
    }

    // TODO: Remove following migration
    @Deprecated
    @JsonCreator
    public static ArtisticDesignSubtypeEnum parse(String candidate) {
        return "Other".equalsIgnoreCase(candidate)
                ? ArtisticDesignSubtypeEnum.OTHER
                : inlineableParseMethod(candidate);
    }

    //@JsonCreator
    public static ArtisticDesignSubtypeEnum inlineableParseMethod(String candidate) {
        return Arrays.stream(ArtisticDesignSubtypeEnum.values())
                .filter(subType -> subType.getType().equalsIgnoreCase(candidate))
                .collect(SingletonCollector.collect());
    }

    public String getType() {
        return type;
    }
}
