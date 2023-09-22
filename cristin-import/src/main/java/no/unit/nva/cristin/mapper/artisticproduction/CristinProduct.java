package no.unit.nva.cristin.mapper.artisticproduction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.instancetypes.artistic.film.realization.MovingPictureOutput;
import no.unit.nva.model.instancetypes.artistic.film.realization.OtherRelease;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArts;
import no.unit.nva.model.instancetypes.artistic.visualarts.VisualArtsSubtype;
import nva.commons.core.StringUtils;


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

    private static final String LINE_BREAK = "\n";

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
            extractOutPuts());
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
    private String[] descriptionFields() {
        return new String[]{
            publisherName,
            publisherPlace,
            ensembleName,
            extractProductionTypeCode(),
            extractFormatCode()
        };
    }

    private String extractFormatCode() {
        return Optional.ofNullable(format).map(CristinFormat::getFormatCode).orElse(null);
    }

    private String extractProductionTypeCode() {
        return Optional.ofNullable(productionType).map(ArtisticProductionType::getProductTypeCode).orElse(null);
    }

    @JsonIgnore
    public VisualArts toVisualArts() {
        return new VisualArts(
            VisualArtsSubtype.createOther(extractVisualArtsOtherSubtypeDescription()),
            null,
            Set.of());
    }

    private String extractVisualArtsOtherSubtypeDescription() {
        var nonEmptyDescriptionFields = Arrays.stream(descriptionFields()).filter(StringUtils::isNotEmpty).toList();
        return String.join(LINE_BREAK, nonEmptyDescriptionFields);
    }
}
