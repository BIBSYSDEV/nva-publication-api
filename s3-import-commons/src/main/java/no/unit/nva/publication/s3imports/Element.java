package no.unit.nva.publication.s3imports;

import javax.xml.bind.annotation.XmlEnumValue;
import nva.commons.core.JacocoGenerated;

public enum Element {

    @XmlEnumValue("identifier") IDENTIFIER("identifier"), @XmlEnumValue("description") DESCRIPTION("description");

    private final String value;

    Element(String value) {
        this.value = value;
    }

    @JacocoGenerated
    public String getValue() {
        return value;
    }
}