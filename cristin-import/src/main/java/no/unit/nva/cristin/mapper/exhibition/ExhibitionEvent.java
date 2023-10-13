package no.unit.nva.cristin.mapper.exhibition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
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
@JsonIgnoreProperties({"arstall", "titteltekst", "antall_deltakere",
    "antall_internasjonale_deltakere", "antall_nasjonale_deltakere", "arrangornavn", "landkode",
    "institusjonsnr_arrangor", "avdnr_arrangor", "undavdnr_arrangor", "gruppenr_arrangor",
    "stedangivelse", "utbredelsesomrade", "url", "personlopenr_arrangor"})
public class ExhibitionEvent {

    @JsonProperty("hendelsestype")
    private MuseumEventCategory museumEventCategory;

    @JsonProperty("dato_til")
    private String dateTo;

    @JsonProperty("dato_fra")
    private String dateFrom;

    @JacocoGenerated
    public ExhibitionEvent() {

    }

    public Period toPeriod() {
        return new Period(Time.convertToInstant(dateFrom), extractToDate());
    }

    public boolean isInfiniteEvent() {
        return StringUtils.isBlank(dateTo);
    }

    private Instant extractToDate() {
        return Objects.nonNull(dateTo)
                   ? Time.convertToInstant(dateTo)
                   : null;
    }
}
