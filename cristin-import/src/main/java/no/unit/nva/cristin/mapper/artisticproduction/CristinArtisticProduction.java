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
import no.unit.nva.cristin.lambda.ErrorReport;
import no.unit.nva.cristin.mapper.DescriptionExtractor;
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
import no.unit.nva.model.instancetypes.artistic.performingarts.PerformingArts;
import no.unit.nva.model.instancetypes.artistic.performingarts.PerformingArtsSubtype;
import no.unit.nva.model.instancetypes.artistic.performingarts.PerformingArtsSubtypeEnum;
import no.unit.nva.model.instancetypes.artistic.performingarts.realization.PerformingArtsOutput;
import no.unit.nva.model.time.Instant;
import no.unit.nva.model.time.duration.DefinedDuration;
import no.unit.nva.model.time.duration.Duration;
import no.unit.nva.model.time.duration.NullDuration;
import no.unit.nva.model.time.duration.UndefinedDuration;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import software.amazon.awssdk.services.s3.S3Client;

import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.mapper.artisticproduction.ArtisticProductionTimeUnit.MINUTE;
import static no.unit.nva.cristin.mapper.artisticproduction.ArtisticProductionTimeUnit.UKE;
import static no.unit.nva.model.instancetypes.artistic.music.MusicMediaType.COMPACT_DISC;
import static no.unit.nva.model.instancetypes.artistic.music.MusicMediaType.DIGITAL_FILE;
import static no.unit.nva.model.instancetypes.artistic.music.MusicMediaType.OTHER;
import static no.unit.nva.model.instancetypes.artistic.music.MusicMediaType.STREAMING;
import static no.unit.nva.model.instancetypes.artistic.music.MusicMediaType.VINYL;
import static nva.commons.core.attempt.Try.attempt;

/*
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
@JsonIgnoreProperties({"status_bestilt", "produkttype"})
@SuppressWarnings({"PMD.TooManyFields", "PMD.GodClass"})
public class CristinArtisticProduction implements DescriptionExtractor, MovingPictureExtractor {

    @JsonIgnore
    public static final String YES = "J";

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
        // NO-OP
    }

    @JsonIgnore
    public MovingPicture toMovingPicture() {
        return new MovingPicture(extractSubType(artisticProductionTimeUnit, duration),
                                 extractDescription(descriptionFields()),
                                 extractOutPuts(),
                                 extractDuration());
    }

    private Duration extractDuration() {
        return nonNull(duration) ? createNonNullDuration() : NullDuration.create();
    }


    private Duration createNonNullDuration() {
        return nonNull(artisticProductionTimeUnit) && nonNull(artisticProductionTimeUnit.getTimeUnitCode())
            ? switch (artisticProductionTimeUnit.getTimeUnitCode()) {
            case UKE -> DefinedDuration.builder().withWeeks(Integer.parseInt(duration)).build();
            case MINUTE -> DefinedDuration.builder().withMinutes(Integer.parseInt(duration)).build();
            default -> UndefinedDuration.fromValue(duration);
        } : UndefinedDuration.fromValue(duration);
    }


    @JsonIgnore
    public MusicPerformance toMusicPerformance(Integer cristinId, S3Client s3Client) {
        return new MusicPerformance(extractMusicPerformanceManifestations(cristinId, s3Client), extractDuration());
    }

    @JsonIgnore
    public PerformingArts toTheatricalPerformance() {
        return new PerformingArts(PerformingArtsSubtype.create(PerformingArtsSubtypeEnum.THEATRICAL_PRODUCTION),
                                  extractDescription(descriptionFields()),
                                  extractTheatricalEvents());
    }

    @JsonIgnore
    public String getDescription() {
        return extractDescription(Stream.of(Optional.ofNullable(crew),
                                            Optional.ofNullable(coCreators)));
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
    private Stream<Optional<String>> descriptionFields() {
        return Stream.of(createInformativeDescription("Ensemble navn: %s", ensembleName),
                         createInformativeDescription("Produsent: %s", producer),
                         createInformativeDescription("Arrangørnavn: %s", extractOrganizerName()),
                         createInformativeDescription("Besetning: %s", crew),
                         createInformativeDescription("Medvirkende: %s", coCreators));
    }

    private List<PerformingArtsOutput> extractTheatricalEvents() {
        var performingArtsOutputs = new ArrayList<PerformingArtsOutput>();
        var venueOptional = Optional.ofNullable(event)
                                .map(ArtisticEvent::toNvaPerformingArtsVenue);
        venueOptional.ifPresent(performingArtsOutputs::add);
        return performingArtsOutputs;
    }

    private List<MusicPerformanceManifestation> extractMusicPerformanceManifestations(Integer cristinId,
                                                                                      S3Client s3Client) {
        var manifestations = new ArrayList<MusicPerformanceManifestation>();
        manifestations.add(extractMusicPerformanceManifestation());
        var audioVisualPublication = extractAudioVisualPublication(cristinId, s3Client);
        audioVisualPublication.ifPresent(manifestations::add);
        var musicScore = extractMusicScore(cristinId, s3Client);
        musicScore.ifPresent(manifestations::add);
        return manifestations;
    }

    private Optional<MusicScore> extractMusicScore(Integer cristinId, S3Client s3Client) {
        if (StringUtils.isNotBlank(ismn) || StringUtils.isNotBlank(ensembleName)) {
            return Optional.of(createMusicScore(cristinId, s3Client));
        }
        return Optional.empty();
    }

    private MusicScore createMusicScore(Integer cristinId, S3Client s3Client) {
        return attempt(() -> new MusicScore(ensembleName,
                                            null, extractExtent(),
                                            new UnconfirmedPublisher(publisherName),
                                            extractIsmn(cristinId, s3Client)))
                   .orElseThrow();
    }

    private Ismn extractIsmn(Integer cristinId, S3Client s3Client) {
        return StringUtils.isNotBlank(ismn) ? createIsmn(cristinId, s3Client) : null;
    }

    private Ismn createIsmn(Integer cristinId, S3Client s3Client) {
        try {
            return new Ismn(ismn);
        } catch (Exception e) {
            ErrorReport.exceptionName(InvalidIsmnException.class.getSimpleName())
                .withBody(ismn)
                .withCristinId(cristinId)
                .persist(s3Client);
            return null;
        }
    }

    private Optional<AudioVisualPublication> extractAudioVisualPublication(Integer cristinId, S3Client s3Client) {
        if (StringUtils.isNotEmpty(isrc) || hasAudioVisualMedium()) {
            return Optional.of(createAudioVisualPublication(cristinId, s3Client));
        }
        return Optional.empty();
    }

    private boolean hasAudioVisualMedium() {
        return nonNull(medium);
    }

    private AudioVisualPublication createAudioVisualPublication(Integer cristinId, S3Client s3Client) {
        return attempt(() -> new AudioVisualPublication(extractMediumType(),
                                                        new UnconfirmedPublisher(publisherName),
                                                        null,
                                                        List.of(),
                                                        constructIsrc(cristinId, s3Client))).orElseThrow();
    }

    private Isrc constructIsrc(Integer cristinId, S3Client s3Client) {
        return Optional.ofNullable(isrc).map(value -> extractIsrc(value, cristinId, s3Client)).orElse(null);
    }

    private Isrc extractIsrc(String isrc, Integer cristinId, S3Client s3Client) {
        return attempt(() -> new Isrc(isrc))
                   .orElse(failure -> persistInvalidIsrcReport(isrc, cristinId, s3Client));
    }

    private Isrc persistInvalidIsrcReport(String isrc, Integer cristinId, S3Client s3Client) {
        ErrorReport.exceptionName(InvalidIsrcException.name())
            .withBody(isrc)
            .withCristinId(cristinId)
            .persist(s3Client);
        return null;
    }

    private MusicMediaSubtype extractMediumType() {
        return new MusicMediaSubtype(convertCristinMediumToNvaMediumSubtype());
    }

    private MusicMediaType convertCristinMediumToNvaMediumSubtype() {
        return Optional.ofNullable(medium).map(this::convertToNvaMediaSubType).orElse(OTHER);
    }

    private MusicMediaType convertToNvaMediaSubType(String medium) {
        if (isCompactDisc(medium)) {
            return COMPACT_DISC;
        }
        if (isVinyl(medium)) {
            return VINYL;
        }
        if (isDigitalFile(medium)) {
            return DIGITAL_FILE;
        }
        if (isStreaming(medium)) {
            return STREAMING;
        }
        return OTHER;
    }

    @JsonIgnore
    private boolean isStreaming(String medium) {
        var pattern = Pattern.compile(
            ".*((strøm)|(Spotify)|(Stream)|(YouTube)|(vimeo))+.*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        var matcher = pattern.matcher(medium);
        return matcher.matches();
    }

    @JsonIgnore
    private boolean isDigitalFile(String medium) {
        var pattern = Pattern.compile(
            ".*((Digital)|(mp3)|(Lydfil))+.*",
            Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(medium);
        return matcher.matches();
    }

    @JsonIgnore
    private boolean isVinyl(String medium) {
        var pattern = Pattern.compile(
            ".*(Plateinnspilling|LP)+.*",
            Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(medium);
        return matcher.matches();
    }

    @JsonIgnore
    private boolean isCompactDisc(String medium) {
        var pattern = Pattern.compile(
            ".*(CD|Album)+.*",
            Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(medium);
        return matcher.matches();
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

    @JsonIgnore
    private boolean isConcert() {
        return Optional.ofNullable(performance).map(Performance::isConcert).orElse(false);
    }

    private String extractExtent() {
        return Optional.ofNullable(artisticProductionTimeUnit)
                   .map(ArtisticProductionTimeUnit::timeUnitIsInMinutes)
                   .map(timeIsInMinutes -> Boolean.TRUE.equals(timeIsInMinutes) ? duration : null)
                   .orElse(null);
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

    private String extractOrganizerName() {
        return Optional.ofNullable(event)
                   .map(ArtisticEvent::getOrganizerName)
                   .orElse(null);
    }
}
