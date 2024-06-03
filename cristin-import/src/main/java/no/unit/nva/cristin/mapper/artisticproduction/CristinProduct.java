package no.unit.nva.cristin.mapper.artisticproduction;

import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.mapper.artisticproduction.ArtisticProductionTimeUnit.MINUTE;
import static no.unit.nva.cristin.mapper.artisticproduction.ArtisticProductionTimeUnit.UKE;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import no.unit.nva.cristin.mapper.DescriptionExtractor;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.instancetypes.artistic.architecture.Architecture;
import no.unit.nva.model.instancetypes.artistic.architecture.ArchitectureSubtype;
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.instancetypes.artistic.film.realization.MovingPictureOutput;
import no.unit.nva.model.instancetypes.artistic.film.realization.OtherRelease;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArts;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArtsSubtype;
import no.unit.nva.model.time.duration.DefinedDuration;
import no.unit.nva.model.time.duration.Duration;
import no.unit.nva.model.time.duration.NullDuration;
import no.unit.nva.model.time.duration.UndefinedDuration;


/*
 * Cristin-categories containing this object:
 * ARKITEKTTEGNING
 * FILMPRODUKSJON
 * KUNST_OG_BILDE
 * ANNET_PRODUKT
 * DATABASE
 * DIGITALE_LÆREM
 * LYDMATERIALE
 * MODELL_ARKITEKT
 * MULTIMEDIAPROD
 * MUSIKK_INNSP
 */

@Builder(
    builderClassName = "CristinProductBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"utbredelsesomrade", "status_bestilt"})
public class CristinProduct implements DescriptionExtractor, MovingPictureExtractor {

    @JsonIgnore
    private static final String PUBLISHER_NAME_DESCRIPTION = "Publisert av: %s";

    @JsonIgnore
    private static final String PUBLISHER_PLACE = "Publiseringssted: %s";

    @JsonIgnore
    private static final String ENSEMBLE_NAME_DESCRIPTION = "Ensemble navn: %s";

    @JsonIgnore
    private static final String PRODUCTION_TYPE_DESCRIPTION = "Produksjons type: %s";

    @JsonIgnore
    private static final String FORMAT_DESCRIPTION = "Format: %s";

    @JsonIgnore
    private static final String MIGRATED_FROM_CRISTIN_MESSAGE = "Migrert fra cristin";

    @JsonProperty("utgivernavn")
    private String publisherName;

    @JsonProperty("antall")
    private String duration;

    @JsonProperty("tidsenhet")
    private ArtisticProductionTimeUnit timeUnit;

    @JsonProperty("produkttype")
    private ArtisticProductionType productionType;

    @JsonProperty("format")
    private CristinFormat format;

    @JsonProperty("utgiversted")
    private String publisherPlace;

    @JsonProperty("isrc")
    private String isrc;

    @JsonProperty("ensembletype")
    private Ensemble ensemble;

    @JsonProperty("ensemblenavn")
    private String ensembleName;

    public CristinProduct() {

    }

    @JsonIgnore
    public MovingPicture toMovingPicture() {
        return new MovingPicture(extractSubType(timeUnit, duration),
                                 extractDescription(descriptionFields()),
                                 extractOutPuts(),
                                 extractDuration());
    }

    private Duration extractDuration() {
        return nonNull(duration) ? createNonNullDuration() : NullDuration.create();
    }


    private Duration createNonNullDuration() {
        return nonNull(timeUnit) && nonNull(timeUnit.getTimeUnitCode())
                   ? switch (timeUnit.getTimeUnitCode()) {
            case UKE -> DefinedDuration.builder().withWeeks(duration).build();
            case MINUTE -> DefinedDuration.builder().withMinutes(duration).build();
            default -> UndefinedDuration.fromValue(duration);
        } : UndefinedDuration.fromValue(duration);
    }

    @JsonIgnore
    public VisualArts toVisualArts() {
        return new VisualArts(
            VisualArtsSubtype.createOther(extractVisualArtsOtherSubtypeDescription()),
            null,
            Set.of());
    }

    public Architecture toArchitecture() {
        return new Architecture(
            ArchitectureSubtype.createOther(MIGRATED_FROM_CRISTIN_MESSAGE),
            extractDescription(descriptionFields()),
            List.of());
    }

    @JsonIgnore
    private List<MovingPictureOutput> extractOutPuts() {
        return List.of(extractOutPut());
    }

    @JsonIgnore
    private OtherRelease extractOutPut() {
        return new OtherRelease(getProductDescription(),
                                new UnconfirmedPlace(publisherPlace, null),
                                new UnconfirmedPublisher(publisherName),
                                null,
                                1);
    }

    @JsonIgnore
    private String getProductDescription() {
        return Optional.of(format)
                   .map(CristinFormat::getFormatCode)
                   .orElse(null);
    }

    @JsonIgnore
    private Stream<Optional<String>> descriptionFields() {
        return Stream.of(
            createInformativeDescription(PUBLISHER_NAME_DESCRIPTION, publisherName),
            createInformativeDescription(PUBLISHER_PLACE, publisherPlace),
            createInformativeDescription(ENSEMBLE_NAME_DESCRIPTION, ensembleName),
            createInformativeDescription(PRODUCTION_TYPE_DESCRIPTION, extractProductionTypeCode()),
            createInformativeDescription(FORMAT_DESCRIPTION, extractFormatCode())
        );
    }

    private String extractFormatCode() {
        return Optional.ofNullable(format).map(CristinFormat::getFormatCode).orElse(null);
    }

    private String extractProductionTypeCode() {
        return Optional.ofNullable(productionType).map(ArtisticProductionType::getProductTypeCode).orElse(null);
    }

    private String extractVisualArtsOtherSubtypeDescription() {
        return extractDescription(descriptionFields());
    }
}
