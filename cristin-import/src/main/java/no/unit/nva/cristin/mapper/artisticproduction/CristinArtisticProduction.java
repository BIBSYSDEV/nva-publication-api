package no.unit.nva.cristin.mapper.artisticproduction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
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
import nva.commons.core.JacocoGenerated;


/**
 * Cristin-categories containing this object:
 * FILMPRODUKSJON
 * MUSIKK_FRAMFORIN
 * MUSIKK_KOMP
 * TEATERPRODUKSJON
 */

@Builder(
    builderClassName = "CristinArtisticProductionBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"status_bestilt", "produkttype", "hendelse"})
@SuppressWarnings({"PMD.TooManyFields"})
public class CristinArtisticProduction implements DescriptionExtractor, MovingPictureExtractor {


    @JsonProperty("status_urframforing")
    private String premiere;

    @JsonProperty("tidsenhet")
    private ArtisticProductionTimeUnit artisticProductionTimeUnit;

    @JsonProperty("antall")
    private String duration;

    @JsonProperty("produksjonstype")
    private ArtisticProductionType artisticProductionType;

    @JsonProperty("framforingstype")
    private Performance performance;

    @JsonProperty("ensembletype")
    private Ensemble ensemble;

    @JsonProperty("ensemblenavn")
    private String ensembleName;

    @JsonProperty("antall_ensemble")
    private int ensembleCount;


    @JsonProperty("utgivernavn")
    private String publisherName;

    @JsonProperty("utgiversted")
    private String publisherPlace;

    @JsonProperty("ismn")
    private String ismn;

    @JsonProperty("originalverk_komponist")
    private String originalComposer;

    @JsonProperty("medskapere")
    private String coCreators;

    @JsonProperty("sjanger")
    private ArtisticGenre artisticGenre;

    @JsonProperty("medium")
    private String medium;

    @JsonProperty("isrc")
    private String isrc;

    @JsonProperty("produsent")
    private String producer;

    @JsonProperty("besetning")
    private String crew;


    @JacocoGenerated
    public CristinArtisticProduction() {

    }

    @JsonIgnore
    public MovingPicture toMovingPicture() {
        return new MovingPicture(extractSubType(artisticProductionTimeUnit, duration),
            extractDescription(descriptionFields()),
            extractOutPuts());
    }

    private List<MovingPictureOutput> extractOutPuts() {
        return List.of(extractOutPut());
    }

    private OtherRelease extractOutPut() {
        return new OtherRelease(medium,
            new UnconfirmedPlace(publisherPlace, null),
            new UnconfirmedPublisher(publisherName),
            null,
            1);
    }

    @JsonIgnore
    private String[] descriptionFields() {
        return new String[]{
            ensembleName,
            producer,
            crew,
            coCreators};
    }


}
