package no.unit.nva.cristin.mapper.artisticproduction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;
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
            ensembleName
        };
    }
}
