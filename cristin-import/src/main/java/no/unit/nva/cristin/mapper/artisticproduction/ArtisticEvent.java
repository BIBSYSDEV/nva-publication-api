package no.unit.nva.cristin.mapper.artisticproduction;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.instancetypes.artistic.performingarts.realization.PerformingArtsVenue;
import no.unit.nva.model.time.Instant;
import no.unit.nva.model.time.Period;
import no.unit.nva.model.time.Time;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;


@Data
@Builder(
    builderClassName = "ArtisticEventBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"hendelsetype", "antall_deltakere", "antall_internasjonale_deltakere",
    "antall_nasjonale_deltakere", "landkode", "institusjonsnr_arrangor", "avdnr_arrangor", "undavdnr_arrangor",
    "gruppenr_arrangor", "utbredelsesomrade", "url", "personlopenr_arrangor"})
public class ArtisticEvent {

    @JsonIgnore
    private static final int WILL_ALWAYS_BE_ONLY_ONE_PERFOMINGARTSVENUE = 0;

    @JsonProperty("arstall")
    private String year;

    @JsonProperty("dato_fra")
    private String dateFrom;

    @JsonProperty("dato_til")
    private String dateTo;

    @JsonProperty("titteltekst")
    private String title;

    @JsonProperty("stedangivelse")
    private String place;

    @JsonProperty("arrangornavn")
    private String organizerName;

    @JacocoGenerated
    public ArtisticEvent() {

    }

    @JsonIgnore
    public UnconfirmedPlace toNvaPlace() {
        return new UnconfirmedPlace(place, null);
    }

    public Instant getNvaTime() {
        return new Instant(Time.convertToInstant(dateFrom));
    }


    @JsonIgnore
    public PerformingArtsVenue toNvaPerformingArtsVenue() {
        return new PerformingArtsVenue(toNvaPlace(),
            extractDate(),
            WILL_ALWAYS_BE_ONLY_ONE_PERFOMINGARTSVENUE);
    }


    @JsonIgnore
    private Time extractDate() {
        if (StringUtils.isNotBlank(dateTo) && StringUtils.isNotBlank(dateFrom)) {
            return new Period(Time.convertToInstant(dateFrom), Time.convertToInstant(dateTo));
        }
        return new Instant(Time.convertToInstant(dateFrom));
    }
}
