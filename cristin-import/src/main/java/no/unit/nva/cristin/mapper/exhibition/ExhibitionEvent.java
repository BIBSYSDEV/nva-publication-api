package no.unit.nva.cristin.mapper.exhibition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import no.unit.nva.model.UnconfirmedOrganization;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.time.Period;
import no.unit.nva.model.time.Time;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

@Builder(
    builderClassName = "ExhibitionEventBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"arstall", "antall_deltakere",
    "antall_internasjonale_deltakere", "antall_nasjonale_deltakere",
    "institusjonsnr_arrangor", "avdnr_arrangor", "undavdnr_arrangor", "gruppenr_arrangor",
    "utbredelsesomrade", "url", "personlopenr_arrangor"})
// antall_internasjonale_deltakere, antall_nasjonale_deltakere, antall_deltakere, url are always null
public class ExhibitionEvent {

    @JsonProperty("hendelsestype")
    private MuseumEventCategory museumEventCategory;

    @JsonProperty("dato_til")
    private String dateTo;

    @JsonProperty("dato_fra")
    private String dateFrom;

    @JsonProperty("arrangornavn")
    private String organizerName;

    @JsonProperty("stedangivelse")
    private String placeDescription;

    @JsonProperty("landkode")
    private String countryCode;

    @JsonProperty("titteltekst")
    private String titleText;

    @JacocoGenerated
    public ExhibitionEvent() {

    }

    public Period toPeriod() {
        return new Period(Time.convertToInstant(dateFrom), extractToDate());
    }

    public boolean isInfiniteEvent() {
        return StringUtils.isBlank(dateTo);
    }

    public UnconfirmedPlace extractPlace() {
        return new UnconfirmedPlace(placeDescription, countryCode);
    }

    public UnconfirmedOrganization extractOrganisation() {
        return new UnconfirmedOrganization(organizerName);
    }

    public String getDescription() {
        return Optional.ofNullable(titleText)
                   .filter("utstilling"::equalsIgnoreCase)
                   .orElse(null);
    }

    private Instant extractToDate() {
        return Optional.ofNullable(dateTo)
                   .map(Time::convertToInstant)
                   .orElse(null);
    }
}
