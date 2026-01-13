package no.unit.nva.publication.s3imports;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@XmlAccessorType(XmlAccessType.FIELD)
public class DcValue {

    @XmlAttribute
    private Element element;

    @XmlAttribute
    private Qualifier qualifier;

    @XmlValue
    private String value;

    public DcValue() {
    }

    public DcValue(Element element, Qualifier qualifier, String value) {
        this.element = element;
        this.qualifier = qualifier;
        this.value = value;
    }

    public Element getElement() {
        return element;
    }

    public void setElement(Element element) {
        this.element = element;
    }

    public Qualifier getQualifier() {
        return qualifier;
    }

    public void setQualifier(Qualifier qualifier) {
        this.qualifier = qualifier;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}