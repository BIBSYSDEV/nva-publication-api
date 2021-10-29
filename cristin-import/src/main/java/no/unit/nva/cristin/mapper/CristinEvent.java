package no.unit.nva.cristin.mapper;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nva.commons.core.JacocoGenerated;

import java.time.LocalDate;

@Data
@Builder(
        builderClassName = "CristinEventBuilder",
        toBuilder = true,
        builderMethodName = "builder",
        buildMethodName = "build",
        setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"antall_deltakere", "antall_internasjonale_deltakere",
        "antall_nasjonale_deltakere", "institusjonsnr_arrangor", "avdnr_arrangor",
        "undavdnr_arrangor", "gruppenr_arrangor", "utbredelsesomrade","arstall", "hendelsetype",
        "url", "personlopenr_arrangor"})
public class CristinEvent {


    public static final String TITLE = "titteltekst";
    public static final String DATE_FROM = "dato_fra";
    public static final String DATE_TO = "dato_til";
    public static final String COUNTRY_CODE = "landkode";
    public static final String ORGANIZER = "arrangornavn";
    public static final String PLACE = "stedangivelse";

    @JsonProperty(TITLE)
    private String title;
    @JsonProperty(DATE_FROM)
    private LocalDate dateFrom;
    @JsonProperty(DATE_TO)
    private LocalDate dateTo;
    @JsonProperty(COUNTRY_CODE)
    private String countryCode;
    @JsonProperty(ORGANIZER)
    private String organizerName;
    @JsonProperty(PLACE)
    private String place;

    @JacocoGenerated
    public CristinEvent() {
    }
}
