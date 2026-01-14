package no.unit.nva.publication.s3imports;

import javax.xml.bind.annotation.XmlEnumValue;
import nva.commons.core.JacocoGenerated;

public enum Language {

    @XmlEnumValue("en_US") ENGLISH("en_US"), @XmlEnumValue("nb_no") NORWEGIAN("nb_no");

    private final String value;

    Language(String value) {
        this.value = value;
    }

    @JacocoGenerated
    public String getValue() {
        return value;
    }

    public String toNvaLanguage() {
        return switch (this) {
            case ENGLISH -> "en";
            case NORWEGIAN -> "nb";
        };
    }
}
