package no.unit.nva.publication.s3imports;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import nva.commons.core.JacocoGenerated;

@XmlType(name = "qualifier")
@XmlEnum
public enum Qualifier {

    @XmlEnumValue("uri") URI("uri");
    //elements collected from this specification: https://dok.unit.no/brage/veiledninger/metadatafelter
    private final String value;

    Qualifier(String value) {
        this.value = value;
    }

    @JacocoGenerated
    public String getValue() {
        return value;
    }
}