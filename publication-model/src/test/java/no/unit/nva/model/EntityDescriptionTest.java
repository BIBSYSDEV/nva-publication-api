package no.unit.nva.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.model.contexttypes.BasicContext;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.GeographicalContent;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.ResearchData;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.instancetypes.Map;
import no.unit.nva.model.instancetypes.artistic.film.MovingPicture;
import no.unit.nva.model.instancetypes.artistic.film.MovingPictureSubtype;
import no.unit.nva.model.instancetypes.artistic.film.MovingPictureSubtypeEnum;
import no.unit.nva.model.instancetypes.artistic.film.realization.Broadcast;
import no.unit.nva.model.instancetypes.artistic.literaryarts.LiteraryArts;
import no.unit.nva.model.instancetypes.artistic.literaryarts.LiteraryArtsSubtype;
import no.unit.nva.model.instancetypes.artistic.literaryarts.LiteraryArtsSubtypeEnum;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsManifestation;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsMonograph;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsWeb;
import no.unit.nva.model.instancetypes.artistic.music.AudioVisualPublication;
import no.unit.nva.model.instancetypes.artistic.music.MusicMediaSubtype;
import no.unit.nva.model.instancetypes.artistic.music.MusicMediaType;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformance;
import no.unit.nva.model.instancetypes.artistic.music.MusicPerformanceManifestation;
import no.unit.nva.model.instancetypes.artistic.music.MusicScore;
import no.unit.nva.model.instancetypes.artistic.music.MusicTrack;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProduction;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProductionSubtype;
import no.unit.nva.model.instancetypes.exhibition.ExhibitionProductionSubtypeEnum;
import no.unit.nva.model.instancetypes.exhibition.manifestations.ExhibitionOtherPresentation;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.JournalReview;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.researchdata.DataSet;
import no.unit.nva.model.testing.EntityDescriptionBuilder;
import no.unit.nva.model.time.Instant;
import no.unit.nva.model.time.duration.DefinedDuration;
import no.unit.nva.model.validation.EntityDescriptionValidationException;
import no.unit.nva.model.validation.EntityDescriptionValidatorImpl;
import nva.commons.core.paths.UriWrapper;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.time.Year;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static no.unit.nva.DatamodelConfig.dataModelObjectMapper;
import static no.unit.nva.model.testing.PublicationGenerator.randomEntityDescription;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.PublicationInstanceBuilder.randomIsmn;
import static no.unit.nva.model.testing.PublicationInstanceBuilder.randomIsrc;
import static no.unit.nva.model.testing.PublicationInstanceBuilder.randomMonographPages;
import static no.unit.nva.model.testing.PublicationInstanceBuilder.randomUnconfirmedPlace;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn13;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Named.named;

class EntityDescriptionTest {

    private static final Javers JAVERS = JaversBuilder.javers().build();
    private static final String SERIAL_PUBLICATION_CHANNEL_URI_TEMPLATE =
            "https://api.nva.sikt.no/publication-channel/serial-publication/12345/%s";
    private static final String PUBLISHER_CHANNEL_URI_TEMPLATE =
            "https://api.nva.sikt.no/publication-channel/publisher/12345/%s";
    public static final DateSeriesPublisher INVALID_DATE_SERIES_PUBLISHER = new DateSeriesPublisher("2022", "2025", "2025");

    private static Stream<Named<PublicationDate>> yearValueProvider() {

        return Stream.of(
                named("Null publication date", null),
                named("Null year value", publicationDateWithYearOnly(null)),
                named("Empty year value", publicationDateWithYearOnly("")),
                named("Whitespace", publicationDateWithYearOnly(" ")),
                named("Future year", publicationDateWithYearOnly(String.valueOf(Year.now().getValue() + 10))),
                named("Random string", publicationDateWithYearOnly(randomString()))
        );
    }

    public static Stream<Class<? extends PublicationContext>> publicationChannelClassProvider() {
        return Stream.of(
                Book.class,
                Degree.class,
                GeographicalContent.class,
                Journal.class,
                Report.class,
                ResearchData.class
        );
    }

    public static Stream<Named<DateSeriesPublisher>> dateUriProvider() {
        return Stream.of(
                named("Series out of bound", new DateSeriesPublisher("2024", "2020", "2024")),
                named("Publisher out of bound", new DateSeriesPublisher("2024", "2024", "2020")),
                named("Series and Publisher out of bound", new DateSeriesPublisher("2022", "2024", "2020"))
        );
    }

    public static Stream<MusicPerformanceManifestation> musicPerformanceManifestationProvider() {
        return Stream.of(
                new AudioVisualPublication(new MusicMediaSubtype(MusicMediaType.DVD),
                        new Publisher(INVALID_DATE_SERIES_PUBLISHER.publisher()),
                        randomString(),
                        List.of(new MusicTrack(randomString(), randomString(), randomString())), randomIsrc()),
                new MusicScore(randomString(), randomString(), randomString(),
                        new Publisher(INVALID_DATE_SERIES_PUBLISHER.publisher()), randomIsmn())
        );
    }

    public static Stream<LiteraryArtsManifestation> literaryArtsManifestationProvider() {
        return Stream.of(
                new LiteraryArtsMonograph(new Publisher(INVALID_DATE_SERIES_PUBLISHER.publisher()),
                        INVALID_DATE_SERIES_PUBLISHER.date, randomIsbn13(), randomMonographPages()),
                new LiteraryArtsWeb(randomUri(), new Publisher(INVALID_DATE_SERIES_PUBLISHER.publisher()),
                        INVALID_DATE_SERIES_PUBLISHER.date())
        );
    }

    @Test
    void shouldCopyEntityDescriptionWithoutDataLoss() {

        var entityDescription = randomEntityDescription(JournalReview.class);
        var copy = entityDescription.copy().build();

        var diff = compareAsObjectNodes(entityDescription, copy);
        assertThat(diff.prettyPrint(), copy, is(equalTo(entityDescription)));
        assertThat(copy, is(not(sameInstance(entityDescription))));
    }

    @Test
    void shouldSortContributorsWhenItsSet() {
        var entityDescription = randomEntityDescription(JournalReview.class);

        var contributor1 = createRandomContributorWithSequence(2);
        var contributor2 = createRandomContributorWithSequence(1);
        entityDescription.setContributors(List.of(contributor1, contributor2));

        assertThat(entityDescription.getContributors().getFirst(), is(equalTo(contributor2)));
    }

    @Test
    void shouldSortContributorsInTheSameOrderIgnoringTheInputedOrderWhenSequencesAreSame() {
        var entityDescription1 = randomEntityDescription(JournalReview.class);
        var entityDescription2 = randomEntityDescription(JournalReview.class);

        var contributor1 = createRandomContributorWithSequence(1);
        var contributor2 = createRandomContributorWithSequence(1);
        var contributor3 = createRandomContributorWithSequence(1);

        entityDescription1.setContributors(List.of(contributor1, contributor2, contributor3));

        var expectedContributor1 = contributor1.copy().withSequence(1).build();
        var expectedContributor2 = contributor2.copy().withSequence(2).build();
        var expectedContributor3 = contributor3.copy().withSequence(3).build();
        entityDescription2.setContributors(List.of(expectedContributor1, expectedContributor2, expectedContributor3));

        var actual = entityDescription1.getContributors();
        var expected = entityDescription2.getContributors();

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldSortContributorsInTheSameOrderIgnoringTheInputedOrderWhenSequencesAreNull() {
        var entityDescription1 = randomEntityDescription(JournalReview.class);

        var contributor1 = createRandomContributorWithSequence(null);
        var contributor2 = createRandomContributorWithSequence(null);
        var contributor3 = createRandomContributorWithSequence(null);

        entityDescription1.setContributors(List.of(contributor1, contributor2, contributor3));

        var expectedContributor1 = contributor1.copy().withSequence(1).build();
        var expectedContributor2 = contributor2.copy().withSequence(2).build();
        var expectedContributor3 = contributor3.copy().withSequence(3).build();

        var actual = entityDescription1.getContributors();
        var expected = List.of(expectedContributor1, expectedContributor2, expectedContributor3);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldProvideContributorSequenceWhenSequenceIsNullAtStart() {
        var entityDescription = randomEntityDescription(JournalReview.class);
        entityDescription.setContributors(Collections.emptyList());

        var contributor1 = createRandomContributorWithSequence(null);
        var contributor1expected = contributor1.copy().withSequence(3).build();
        var contributor2 = createRandomContributorWithSequence(1);
        var contributor3 = createRandomContributorWithSequence(1);
        var contributor4 = createRandomContributorWithSequence(2);

        var contributors = List.of(contributor1, contributor2, contributor3, contributor4);
        entityDescription.setContributors(contributors);

        var expectedEntityDescription = entityDescription.copy().build();
        var expectedContributors = List.of(contributor2, contributor3, contributor4, contributor1expected);
        expectedEntityDescription.setContributors(expectedContributors);

        assertThat(entityDescription, is(equalTo(expectedEntityDescription)));
    }

    @Test
    void shouldProvideContributorSequenceWhenSequenceIsNullInBetween() {
        var entityDescription = randomEntityDescription(JournalReview.class);
        entityDescription.setContributors(Collections.emptyList());

        var contributor1 = createRandomContributorWithSequence(1);
        var contributor2 = createRandomContributorWithSequence(1);
        var contributor3 = createRandomContributorWithSequence(null);
        var contributor4 = createRandomContributorWithSequence(3);

        entityDescription.setContributors(List.of(contributor1, contributor2, contributor3, contributor4));

        var contributor2expected = contributor2.copy().withSequence(2).build();
        var contributor3expected = contributor3.copy().withSequence(4).build();
        var expectedList = List.of(contributor1, contributor2expected, contributor4, contributor3expected);

        var actual = entityDescription.getContributors();

        assertThat(actual, is(equalTo(expectedList)));
    }

    private static Contributor createRandomContributorWithSequence(Integer integer) {
        return EntityDescriptionBuilder.randomContributorWithSequence(integer);
    }

    @Test
    void shouldProvideContributorSequenceWhenSequenceIsNullAtEnd() {
        var entityDescription = randomEntityDescription(JournalReview.class);
        entityDescription.setContributors(Collections.emptyList());

        var contributor1 = createRandomContributorWithSequence(1);
        var contributor2 = createRandomContributorWithSequence(1);
        var contributor3 = createRandomContributorWithSequence(2);
        var contributor4 = createRandomContributorWithSequence(null);

        entityDescription.setContributors(List.of(contributor1, contributor2, contributor3, contributor4));

        var expectedContributor1 = contributor1.copy().withSequence(1).build();
        var expectedContributor2 = contributor2.copy().withSequence(2).build();
        var expectedContributor3 = contributor3.copy().withSequence(3).build();
        var expectedContributor4 = contributor4.copy().withSequence(4).build();


        var actual = entityDescription.getContributors();
        var expected = List.of(expectedContributor1, expectedContributor2, expectedContributor3, expectedContributor4);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldMaintainContributorSequence() {
        var entityDescription = randomEntityDescription(JournalReview.class);
        entityDescription.setContributors(Collections.emptyList());

        var contributor1 = createRandomContributorWithSequence(1);
        var contributor2 = createRandomContributorWithSequence(2);
        var contributor3 = createRandomContributorWithSequence(3);
        var contributor4 = createRandomContributorWithSequence(4);

        entityDescription.setContributors(List.of(contributor1, contributor2, contributor3, contributor4));

        var actual = entityDescription.getContributors();
        var expected = List.of(contributor1, contributor2, contributor3, contributor4);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldProvideContributorSequenceAndKeepStableSorting() {
        var entityDescription = randomEntityDescription(JournalReview.class);
        entityDescription.setContributors(Collections.emptyList());

        var contributor1 = createContributor(1, "1a");
        var contributor2 = createContributor(1, "1b");
        var expectedContributor2 = createContributor(2, "1b");
        var contributor3 = createContributor(null, "3");
        var expectedContributor3 = createContributor(3, "3");

        entityDescription.setContributors(List.of(contributor1, contributor2, contributor3));

        var expectedList = List.of(contributor1, expectedContributor2, expectedContributor3);

        assertThat(entityDescription.getContributors(), is(equalTo(expectedList)));
    }

    @ParameterizedTest
    @DisplayName("Should throw when publication date is not synchronized with publication channel date")
    @MethodSource("yearValueProvider")
    void shouldThrowWhenPublicationDateIsNotSynchronizedWithPublicationChannelDate(PublicationDate value) {
        var entityDescription = randomEntityDescription(AcademicArticle.class);
        entityDescription.setPublicationDate(value);
        var reference = entityDescription.getReference();
        reference.setPublicationContext(new Journal(URI.create(SERIAL_PUBLICATION_CHANNEL_URI_TEMPLATE.formatted(Year.now().getValue()))));
        var validator = new EntityDescriptionValidatorImpl();
        var validationResult = validator.validate(entityDescription);
        assertThat(validationResult.errors(), is(not(empty())));
    }

    @ParameterizedTest
    @DisplayName("Should throw when publication channel date is not synchronized with publication date")
    @MethodSource("yearValueProvider")
    void shouldThrowWhenPublicationChannelDateIsNotSynchronizedWithPublicationDate(PublicationDate value) {
        var entityDescription = randomEntityDescription(AcademicArticle.class);
        var reference = entityDescription.getReference();
        var channelUri = URI.create(SERIAL_PUBLICATION_CHANNEL_URI_TEMPLATE.formatted(value));
        reference.setPublicationContext(new Journal(channelUri));
        entityDescription.setPublicationDate(publicationDateWithYearOnly(String.valueOf(Year.now().getValue())));
        var validator = new EntityDescriptionValidatorImpl();
        var validationResult = validator.validate(entityDescription);
        assertThat(validationResult.errors(), is(not(empty())));
    }

    @ParameterizedTest
    @MethodSource("publicationChannelClassProvider")
    void shouldNotThrowWhenPublicationDateIsSynchronizedWithPublicationChannelDate(Class<? extends BasicContext> clazz) {
        var publicationYear = String.valueOf(Year.now().getValue());
        var entityDescription = createEntityDescriptionForTypeWithChannelAndPublicationDate(clazz, publicationYear, publicationYear);
        var validator = new EntityDescriptionValidatorImpl();
        var validationResult = validator.validate(entityDescription);
        assertThat(validationResult.isValid(), is(true));
    }

    @ParameterizedTest
    @MethodSource("publicationChannelClassProvider")
    void shouldNotThrowWhenPublicationDateIsNotSynchronizedWithPublicationChannelDate(
            Class<? extends BasicContext> clazz) {
        var year = Year.now();
        var publicationYear = String.valueOf(year.getValue());
        var publicationChannelYear = String.valueOf(Year.now().getValue() + 1);
        var entityDescription = createEntityDescriptionForTypeWithChannelAndPublicationDate(clazz, publicationYear, publicationChannelYear);
        var validator = new EntityDescriptionValidatorImpl();
        var validationResult = validator.validate(entityDescription);
        assertThat(validationResult.errors(), is(not(empty())));
    }

    @ParameterizedTest
    @DisplayName("Should throw when Book series or publisher uris are not synchronized with date")
    @MethodSource("dateUriProvider")
    void shouldThrowWhenBookSeriesAndPublisherAreNotSynchronizedWithDate(DateSeriesPublisher dateSeriesPublisher) {
        var entityDescription = entityDescriptionFromContext(Book.class);
        entityDescription.setPublicationDate(dateSeriesPublisher.date());
        var reference = entityDescription.getReference();
        var book = new Book(new Series(dateSeriesPublisher.series()),
                randomString(),
                new Publisher(dateSeriesPublisher.publisher()),
                List.of(randomIsbn13()),
                Revision.UNREVISED);
        reference.setPublicationContext(book);
        var validator = new EntityDescriptionValidatorImpl();
        var validationResult = validator.validate(entityDescription);
        assertThat(validationResult.errors(), is(not(empty())));
    }

    @Test
    void shouldNotThrowWhenDateSeriesPublisherAreSynchronized() {
        var entityDescription = entityDescriptionFromContext(Book.class);
        var dateSeriesPublisher = new DateSeriesPublisher("2025", "2025", "2025");
        entityDescription.setPublicationDate(dateSeriesPublisher.date());
        var reference = entityDescription.getReference();
        var book = new Book(new Series(dateSeriesPublisher.series()),
                randomString(),
                new Publisher(dateSeriesPublisher.publisher()),
                List.of(randomIsbn13()),
                Revision.UNREVISED);
        reference.setPublicationContext(book);
        var validator = new EntityDescriptionValidatorImpl();
        var validationResult = validator.validate(entityDescription);
        assertThat(validationResult.isValid(), is(true));
    }

    @Test
    void shouldThrowWhenPublicationDateIsNotSynchronized() {
        var entityDescription = entityDescriptionFromContext(Book.class);
        var dateSeriesPublisher = INVALID_DATE_SERIES_PUBLISHER;
        entityDescription.setPublicationDate(dateSeriesPublisher.date());
        var reference = entityDescription.getReference();
        var book = new Book(new Series(dateSeriesPublisher.series()),
                randomString(),
                new Publisher(dateSeriesPublisher.publisher()),
                List.of(randomIsbn13()),
                Revision.UNREVISED);
        reference.setPublicationContext(book);
        assertThrows(EntityDescriptionValidationException.class, entityDescription::validate);
    }

    @Test
    void shouldHandleEntityDescriptionWithoutPublicationContextUris() throws InvalidIssnException {
        var entityDescription = entityDescriptionFromContext(Book.class);
        var dateSeriesPublisher = INVALID_DATE_SERIES_PUBLISHER;
        entityDescription.setPublicationDate(dateSeriesPublisher.date());
        var reference = entityDescription.getReference();
        var book = new Book(new UnconfirmedSeries(randomString(), randomIssn(), randomIssn()),
                randomString(),
                new UnconfirmedPublisher(randomString()),
                List.of(randomIsbn13()),
                Revision.UNREVISED);
        reference.setPublicationContext(book);
        assertDoesNotThrow(entityDescription::validate);
    }

    @ParameterizedTest(name = "Should throw when publication channel URI is invalid: {0}")
    @ValueSource(strings = {"https://example.com", "https://example.com/abc", "https://example.com/1234/abc"})
    void shouldHandleInvalidPublicationContextUris(String uri) throws InvalidIssnException {
        var entityDescription = entityDescriptionFromContext(Book.class);
        var dateSeriesPublisher = new DateSeriesPublisher(randomString(), "2025", "2025");
        entityDescription.setPublicationDate(dateSeriesPublisher.date());
        var reference = entityDescription.getReference();
        var book = new Book(new UnconfirmedSeries(randomString(), randomIssn(), randomIssn()),
                randomString(),
                new Publisher(UriWrapper.fromUri(uri).getUri()),
                List.of(randomIsbn13()),
                Revision.UNREVISED);
        reference.setPublicationContext(book);
        assertThrows(EntityDescriptionValidationException.class, entityDescription::validate);
    }

    @Test
    void shouldThrowWhenContextIsResearchDataAndPublicationChannelUriIsInvalid() {
        var entityDescription = entityDescriptionFromContext(ResearchData.class);
        entityDescription.setPublicationDate(INVALID_DATE_SERIES_PUBLISHER.date());
        var reference = entityDescription.getReference();
        reference.setPublicationContext(new ResearchData(new Publisher(INVALID_DATE_SERIES_PUBLISHER.publisher)));
        assertThrows(EntityDescriptionValidationException.class, entityDescription::validate);
    }

    @Test
    void shouldThrowWhenBroadcastHasUnsynchronizedDateInUri() {
        var entityDescription = randomEntityDescription(MovingPicture.class);
        entityDescription.setPublicationDate(INVALID_DATE_SERIES_PUBLISHER.date());
        var reference = entityDescription.getReference();
        var broadcast = new Broadcast(new Publisher(INVALID_DATE_SERIES_PUBLISHER.publisher()), new Instant(java.time.Instant.now()), 1);
        var instance = new MovingPicture(MovingPictureSubtype.fromJson(MovingPictureSubtypeEnum.FILM, null), randomString(), List.of(broadcast), DefinedDuration.builder().withHours(1).withMinutes(10).build());
        reference.setPublicationInstance(instance);
        assertThrows(EntityDescriptionValidationException.class, entityDescription::validate);
    }

    @ParameterizedTest
    @MethodSource("literaryArtsManifestationProvider")
    void shouldThrowWhenLiteraryArtsHasUnsynchronizedDateInUri(LiteraryArtsManifestation literaryArtsManifestation) {
        var entityDescription = randomEntityDescription(LiteraryArts.class);
        entityDescription.setPublicationDate(INVALID_DATE_SERIES_PUBLISHER.date());
        var reference = entityDescription.getReference();
        var instance = new LiteraryArts(LiteraryArtsSubtype.create(LiteraryArtsSubtypeEnum.NOVEL),
                List.of(literaryArtsManifestation), randomString());
        reference.setPublicationInstance(instance);
        assertThrows(EntityDescriptionValidationException.class, entityDescription::validate);
    }

    @ParameterizedTest
    @MethodSource("musicPerformanceManifestationProvider")
    void shouldThrowWhenMusicPerformanceContainsUnsynchronizedDateInUri(MusicPerformanceManifestation manifestation) {
        var entityDescription = randomEntityDescription(MusicPerformance.class);
        entityDescription.setPublicationDate(INVALID_DATE_SERIES_PUBLISHER.date());
        var reference = entityDescription.getReference();
        var instance = new MusicPerformance(List.of(manifestation), DefinedDuration.builder()
                .withHours(1).withMinutes(10).build());
        reference.setPublicationInstance(instance);
        assertThrows(EntityDescriptionValidationException.class, entityDescription::validate);
    }

    @Test
    void shouldThrowWhenExhibitionProductionHasUnsynchronizedDateInUri() {
        var entityDescription = randomEntityDescription(ExhibitionProduction.class);
        entityDescription.setPublicationDate(INVALID_DATE_SERIES_PUBLISHER.date());
        var reference = entityDescription.getReference();
        var instance = new ExhibitionProduction(new ExhibitionProductionSubtype(ExhibitionProductionSubtypeEnum.OTHER),
                List.of(new ExhibitionOtherPresentation(randomString(), randomString(), randomUnconfirmedPlace(),
                        new Publisher(INVALID_DATE_SERIES_PUBLISHER.publisher()),
                        new Instant(java.time.Instant.now()))));
        reference.setPublicationInstance(instance);
        assertThrows(EntityDescriptionValidationException.class, entityDescription::validate);
    }



    private static EntityDescription createEntityDescriptionForTypeWithChannelAndPublicationDate(
            Class<? extends PublicationContext> clazz, String publicationYear, String publicationChannelYear) {
        var entityDescription = entityDescriptionFromContext(clazz);
        var reference = entityDescription.getReference();
        reference.setPublicationContext(createContextForType(reference, publicationChannelYear));
        entityDescription.setPublicationDate(publicationDateWithYearOnly(publicationYear));
        return entityDescription;
    }

    private static PublicationContext createContextForType(Reference reference, String publicationChannelYear) {
        return switch (reference.getPublicationContext()) {
            case Degree degree -> createBookSubclassWithSeriesAndPublisher(degree, publicationChannelYear);
            case Report report -> createBookSubclassWithSeriesAndPublisher(report, publicationChannelYear);
            case Book book -> createBookSubclassWithSeriesAndPublisher(book, publicationChannelYear);
            case GeographicalContent ignored -> new GeographicalContent(publisher(publicationChannelYear));
            case Journal ignored -> new Journal(serialPublicationUri(publicationChannelYear));
            case ResearchData ignored -> new ResearchData(publisher(publicationChannelYear));
            case null -> throw new IllegalStateException("PublicationContext cannot be null");
            default -> throw new IllegalStateException("Unexpected value: " + reference.getPublicationContext());
        };
    }

    private static Book createBookSubclassWithSeriesAndPublisher(Book book, String publicationYear) {
        return book.copy()
                .withSeries(seriesUri(publicationYear)).withPublisher(publisher(publicationYear)).build();
    }

    private static EntityDescription entityDescriptionFromContext(Class<? extends PublicationContext> clazz) {
        if (clazz.equals(Book.class)) {
            return randomEntityDescription(AcademicMonograph.class);
        } else if (clazz.equals(Degree.class)) {
            return randomEntityDescription(DegreePhd.class);
        } else if (clazz.equals(GeographicalContent.class)) {
            return randomEntityDescription(Map.class);
        } else if (clazz.equals(Journal.class)) {
            return randomEntityDescription(AcademicArticle.class);
        } else if (clazz.equals(Report.class)) {
            return randomEntityDescription(ReportResearch.class);
        } else if (clazz.equals(ResearchData.class)) {
            return randomEntityDescription(DataSet.class);
        } else {
            throw new IllegalStateException("Unexpected value: " + clazz);
        }
    }

    private static PublishingHouse publisher(String value) {
        return new Publisher(URI.create(PUBLISHER_CHANNEL_URI_TEMPLATE.formatted(value)));
    }

    private static BookSeries seriesUri(String value) {
        return new Series(serialPublicationUri(value));
    }

    private static URI serialPublicationUri(String value) {
        return URI.create(SERIAL_PUBLICATION_CHANNEL_URI_TEMPLATE.formatted(value));
    }

    private static Contributor createContributor(Integer integer, String name) {
        return new Contributor.Builder()
                .withSequence(integer)
                .withIdentity(new Identity.Builder()
                        .withName(name)
                        .build())
                .build();
    }

    private Diff compareAsObjectNodes(EntityDescription original, EntityDescription copy) {
        var originalObjectNode = dataModelObjectMapper.convertValue(original, ObjectNode.class);
        var copyObjectNode = dataModelObjectMapper.convertValue(copy, ObjectNode.class);
        return JAVERS.compare(originalObjectNode, copyObjectNode);
    }

    private static PublicationDate publicationDateWithYearOnly(String year) {
        return new PublicationDate.Builder().withYear(year).build();
    }

    record DateSeriesPublisher(PublicationDate date, URI series, URI publisher) {
        public DateSeriesPublisher(String year, String seriesYear, String publisherYear) {
            this(new PublicationDate.Builder().withYear(year).build(),
                    URI.create("https://example.org/serial-publication/"+ UUID.randomUUID() + "/" + seriesYear),
                    URI.create("https://example.org/publisher/"+ UUID.randomUUID() + "/" + publisherYear));
        }
    }
}