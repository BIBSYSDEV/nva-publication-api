package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import nva.commons.core.JacocoGenerated;

@Builder(
    builderClassName = "EventBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"arstall", "hendelsetype", "antall_deltakere",
    "antall_internasjonale_deltakere", "antall_nasjonale_deltakere",
    "institusjonsnr_arrangor", "avdnr_arrangor", "undavdnr_arrangor", "gruppenr_arrangor",
    "utbredelsesomrade", "url", "personlopenr_arrangor"})
public class PresentationEvent {

    public static final String TITLE = "titteltekst";
    public static final String FROM_DATE = "dato_fra";
    private static final String TO_DATE = "dato_til";
    private static final String AGENT = "arrangornavn";
    private static final String COUNTRY_CODE = "landkode";
    public static final String PLACE = "stedangivelse";

    @JsonProperty(TITLE)
    private String title;

    @JsonProperty(FROM_DATE)
    private String from;

    @JsonProperty(TO_DATE)
    private String to;

    @JsonProperty(AGENT)
    private String agent;

    @JsonProperty(COUNTRY_CODE)
    private String countryCode;

    @JsonProperty(PLACE)
    private String place;

    @JacocoGenerated
    public PresentationEvent() {
    }

    @JacocoGenerated
    public PresentationEvent.EventBuilder copy() {
        return this.toBuilder();
    }
}
