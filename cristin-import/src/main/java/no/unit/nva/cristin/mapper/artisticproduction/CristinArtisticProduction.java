package no.unit.nva.cristin.mapper.artisticproduction;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.cristin.mapper.nva.exceptions.InvalidIsrcException;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.instancetypes.artistic.film.realization.MovingPictureOutput;
import no.unit.nva.model.instancetypes.artistic.film.realization.OtherRelease;
import no.unit.nva.model.instancetypes.artistic.music.AudioVisualPublication;
import no.unit.nva.model.instancetypes.artistic.music.Concert;
import no.unit.nva.model.instancetypes.artistic.music.InvalidIsmnException;
import no.unit.nva.model.instancetypes.artistic.music.Ismn;
import no.unit.nva.model.instancetypes.artistic.music.Isrc;
import no.unit.nva.model.instancetypes.artistic.music.MusicMediaSubtype;
import no.unit.nva.model.instancetypes.artistic.music.MusicMediaType;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformanceManifestation;
import no.unit.nva.model.instancetypes.artistic.music.MusicScore;
import no.unit.nva.model.instancetypes.artistic.music.MusicalWork;
import no.unit.nva.model.instancetypes.artistic.music.MusicalWorkPerformance;
import no.unit.nva.model.instancetypes.artistic.music.OtherPerformance;
import no.unit.nva.model.time.Instant;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.unit.nva.model.instancetypes.artistic.music.MusicMediaType.COMPACT_DISC;
import static no.unit.nva.model.instancetypes.artistic.music.MusicMediaType.DIGITAL_FILE;
import static no.unit.nva.model.instancetypes.artistic.music.MusicMediaType.OTHER;
import static no.unit.nva.model.instancetypes.artistic.music.MusicMediaType.VINYL;
import static nva.commons.core.attempt.Try.attempt;


/**
 * Cristin-categories containing this object:
 * FILMPRODUKSJON
 * MUSIKK_FRAMFORIN
 * MUSIKK_KOMP
 * TEATERPRODUKSJON
 */

@Data
@Builder(
    builderClassName = "CristinArtisticProductionBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"status_bestilt", "produkttype"})
@SuppressWarnings({"PMD.TooManyFields"})
public class CristinArtisticProduction implements DescriptionExtractor, MovingPictureExtractor {

    public static final String YES = "J";
    @JsonIgnore
    private static final Map<String, MusicMediaType> CRISTIN_MEDIUM_TO_NVA_MEDIUM =
        Map.of("CD", COMPACT_DISC,
            "CD-innspilling", COMPACT_DISC,
            "Plateinnspilling", VINYL,
            "Digital singel", DIGITAL_FILE,
            "Digitalt album", DIGITAL_FILE);
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

    @JsonProperty("hendelse")
    private ArtisticEvent event;


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


    @JsonIgnore
    public MusicPerformance toMusicPerformance() {
        return new MusicPerformance(extractMusicPerformanceManifestations());
    }

    private List<MusicPerformanceManifestation> extractMusicPerformanceManifestations() {
        var manifestations = new ArrayList<MusicPerformanceManifestation>();
        manifestations.add(extractMusicPerformanceManifestation());
        var audioVisualPublication = extractAudioVisualPublication();
        audioVisualPublication.ifPresent(manifestations::add);
        var musicScore = extractMusicScore();
        musicScore.ifPresent(manifestations::add);
        return manifestations;
    }

    private Optional<MusicScore> extractMusicScore() {
        if (StringUtils.isNotBlank(ismn) || StringUtils.isNotBlank(ensembleName)) {
            return Optional.of(createMusicScore());
        }
        return Optional.empty();
    }

    private MusicScore createMusicScore() {
        return attempt(() -> new MusicScore(ensembleName,
            null, extractExtent(),
            new UnconfirmedPublisher(publisherName),
            extractIsmn()))
            .orElseThrow();
    }

    private Ismn extractIsmn() throws InvalidIsmnException {
        return StringUtils.isNotBlank(ismn) ? new Ismn(ismn) : null;
    }

    private Optional<AudioVisualPublication> extractAudioVisualPublication() {
        if (StringUtils.isNotEmpty(isrc) || "CD".equals(medium)) {
            return Optional.of(createAudioVisualPublication());
        }
        return Optional.empty();
    }

    private AudioVisualPublication createAudioVisualPublication() {
        return attempt(() -> new AudioVisualPublication(extractMediumType(),
            new UnconfirmedPublisher(publisherName),
            null,
            List.of(),
            constructIsrc())).orElseThrow();
    }

    private Isrc constructIsrc() {
        return Optional.of(isrc).map(this::extractIsrc).orElse(null);
    }

    private Isrc extractIsrc(String isrc) {
        return attempt(() -> new Isrc(isrc)).orElseThrow(r -> new InvalidIsrcException(r.getException()));
    }

    private MusicMediaSubtype extractMediumType() {
        return new MusicMediaSubtype(convertCristinMediumToNvaMediumSubtype());
    }

    private MusicMediaType convertCristinMediumToNvaMediumSubtype() {
        return Optional.ofNullable(medium).map(CRISTIN_MEDIUM_TO_NVA_MEDIUM::get).orElse(OTHER);
    }

    private MusicPerformanceManifestation extractMusicPerformanceManifestation() {
        return isConcert()
            ? new Concert(extractPlace(),
            extractTime(),
            extractExtent(),
            extractConcertProgrammes(),
            null)
            : new OtherPerformance(null,
            extractPlace(),
            extractExtent(),
            extractProgrammes());
    }

    private List<MusicalWork> extractProgrammes() {
        return List.of(extractMusicalWork());
    }

    private MusicalWork extractMusicalWork() {
        return new MusicalWork(extractTitle(), originalComposer);
    }

    private boolean isConcert() {
        return Optional.ofNullable(performance).map(Performance::isConcert).orElse(false);
    }

    private String extractExtent() {
        var shouldExtractExtent = Optional.ofNullable(artisticProductionTimeUnit)
            .map(ArtisticProductionTimeUnit::timeUnitIsInMinutes)
            .orElse(false);
        return shouldExtractExtent ? duration : null;
    }

    private List<MusicalWorkPerformance> extractConcertProgrammes() {
        return List.of(extractMusicalWorkPerformance());
    }

    private MusicalWorkPerformance extractMusicalWorkPerformance() {
        return new MusicalWorkPerformance(extractTitle(), originalComposer, isPremiere());
    }

    private String extractTitle() {
        return Optional.ofNullable(event).map(ArtisticEvent::getTitle).orElse(null);
    }

    @JsonIgnore
    private boolean isPremiere() {
        return Optional.ofNullable(premiere).map(YES::equals).orElse(false);
    }

    private Instant extractTime() {
        return Optional.ofNullable(event).map(ArtisticEvent::getNvaTime).orElse(null);
    }

    private UnconfirmedPlace extractPlace() {
        return Optional.ofNullable(event).map(ArtisticEvent::toNvaPlace).orElse(null);
    }
}
