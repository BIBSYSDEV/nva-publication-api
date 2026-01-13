package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.Element.IDENTIFIER;
import static nva.commons.core.attempt.Try.attempt;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "dublin_core")
@XmlAccessorType(XmlAccessType.FIELD)
public class DublinCore {

    private static final JAXBContext JAXB_CONTEXT = createJaxbContext();

    @XmlElement(name = "dcvalue")
    private List<DcValue> dcValues;

    public DublinCore() {
    }

    public DublinCore(List<DcValue> dcValues) {
        this.dcValues = dcValues;
    }

    public static DublinCore fromString(String value) {
        return attempt(() -> unmarshallValue(value)).orElseThrow();
    }

    public Optional<DcValue> getHandle() {
        return dcValues.stream().filter(DublinCore::isHandle).findFirst();
    }

    private static JAXBContext createJaxbContext() {
        try {
            return JAXBContext.newInstance(DublinCore.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to create JAXBContext for DublinCore", e);
        }
    }

    private static DublinCore unmarshallValue(String value) throws JAXBException {
        var unmarshaller = JAXB_CONTEXT.createUnmarshaller();
        return (DublinCore) unmarshaller.unmarshal(new StringReader(value));
    }

    private static boolean isHandle(DcValue dcValue) {
        return IDENTIFIER.equals(dcValue.getElement()) && Qualifier.URI.equals(dcValue.getQualifier());
    }
}