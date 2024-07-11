package no.unit.nva.model.testing;

import static no.unit.nva.model.testing.RandomUtils.randomLabel;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn10;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn13;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import no.unit.nva.model.Agent;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Revision;
import no.unit.nva.model.UnconfirmedCourse;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Artistic;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.ExhibitionContent;
import no.unit.nva.model.contexttypes.GeographicalContent;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.MediaContribution;
import no.unit.nva.model.contexttypes.MediaContributionPeriodical;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.ResearchData;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedMediaContributionPeriodical;
import no.unit.nva.model.contexttypes.media.MediaFormat;
import no.unit.nva.model.contexttypes.media.MediaSubType;
import no.unit.nva.model.contexttypes.media.MediaSubTypeEnum;
import no.unit.nva.model.contexttypes.media.MediaSubTypeOther;
import no.unit.nva.model.contexttypes.media.SeriesEpisode;
import no.unit.nva.model.contexttypes.place.Place;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.time.Period;
import no.unit.nva.model.time.Time;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD.CouplingBetweenObjects")
@JacocoGenerated
public class PublicationContextBuilder {

    @SuppressWarnings("PMD.NcssCount")
    public static PublicationContext randomPublicationContext(Class<?> publicationInstance) {
        String className = publicationInstance.getSimpleName();
        switch (className) {
            case "Architecture":
            case "ArtisticDesign":
            case "MovingPicture":
            case "PerformingArts":
            case "MusicPerformance":
            case "VisualArts":
            case "LiteraryArts":
                return randomArtistic();
            case "JournalIssue":
            case "ConferenceAbstract":
            case "FeatureArticle":
            case "JournalCorrigendum":
            case "JournalArticle":
            case "AcademicArticle":
            case "AcademicLiteratureReview":
            case "CaseReport":
            case "StudyProtocol":
            case "ProfessionalArticle":
            case "PopularScienceArticle":
            case "JournalInterview":
            case "JournalLetter":
            case "JournalLeader":
            case "JournalReview":
                return randomJournal();
            case "AcademicMonograph":
            case "Encyclopedia":
            case "ExhibitionCatalog":
            case "NonFictionMonograph":
            case "PopularScienceMonograph":
            case "Textbook":
            case "BookAnthology":
            case "BookAbstracts":
            case "BookMonograph":
            case "OtherStudentWork":
                return attempt(PublicationContextBuilder::randomBook).orElseThrow();
            case "DegreeBachelor":
            case "DegreeMaster":
            case "DegreePhd":
            case "DegreeLicentiate":
                return attempt(PublicationContextBuilder::randomDegree).orElseThrow();
            case "ChapterArticle":
            case "AcademicChapter":
            case "NonFictionChapter":
            case "PopularScienceChapter":
            case "TextbookChapter":
            case "EncyclopediaChapter":
            case "Introduction":
            case "ExhibitionCatalogChapter":
            case "ChapterConferenceAbstract":
            case "ChapterInReport":
                return randomChapter();
            case "ReportBasic":
            case "ReportPolicy":
            case "ReportResearch":
            case "ReportWorkingPaper":
            case "ReportBookOfAbstract":
            case "ConferenceReport":
                return attempt(PublicationContextBuilder::randomReport).orElseThrow();
            case "ConferenceLecture":
            case "ConferencePoster":
            case "Lecture":
            case "OtherPresentation":
                return randomPresentation();
            case "MediaFeatureArticle":
            case "MediaReaderOpinion":
                return randomMediaContributionPeriodical();
            case "MediaBlogPost":
            case "MediaInterview":
            case "MediaParticipationInRadioOrTv":
            case "MediaPodcast":
                return randomMediaContribution();
            case "DataManagementPlan":
            case "DataSet":
                return randomResearchData();
            case "Map":
                return randomGeographicalContent();
            case "ExhibitionProduction":
                return randomExhibition();
            default:
                throw new UnsupportedOperationException("Publication instance not supported: " + className);
        }
    }

    private static ExhibitionContent randomExhibition() {
        return new ExhibitionContent();
    }

    private static GeographicalContent randomGeographicalContent() {
        return new GeographicalContent(randomPublishingHouse());
    }

    private static ResearchData randomResearchData() {
        return new ResearchData(randomPublishingHouse());
    }

    private static PublicationContext randomMediaContributionPeriodical() {
        return randomBoolean()
            ? new MediaContributionPeriodical(randomPublicationChannelsUri())
            : attempt(() -> new UnconfirmedMediaContributionPeriodical(randomString(),
                randomIssn(), randomIssn())).orElseThrow();
    }

    private static MediaContribution randomMediaContribution() {
        return new MediaContribution.Builder()
            .withMedium(generateRandomMedium())
            .withFormat(generateRandomMediaFormat())
            .withDisseminationChannel(randomString())
            .withPartOf(generateRandomSeriesEpisode())
            .build();
    }

    private static SeriesEpisode generateRandomSeriesEpisode() {
        return new SeriesEpisode(randomString(), randomString());
    }

    private static MediaFormat generateRandomMediaFormat() {
        return randomElement(MediaFormat.values());
    }

    private static MediaSubType generateRandomMedium() {
        var type = randomElement(MediaSubTypeEnum.values());
        return MediaSubTypeEnum.OTHER == type
                ? MediaSubTypeOther.createOther(randomString())
                : MediaSubType.create(type);
    }

    private static Degree randomDegree() throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Degree.Builder()
            .withSeriesNumber(randomSeriesNumber())
            .withSeries(randomBookSeries())
            .withIsbnList(randomIsbnList())
            .withPublisher(randomPublishingHouse())
            .withCourse(new UnconfirmedCourse(randomString()))
            .build();
    }

    private static Event randomPresentation() {
        return new Event.Builder()
            .withAgent(randomAgent())
            .withLabel(randomLabel())
            .withPlace(randomPlace())
            .withProduct(randomUri())
            .withTime(randomTime())
            .build();
    }

    private static Time randomTime() {
        Instant from = randomInstant();

        return Math.random() <= 0.5
                   ? new no.unit.nva.model.time.Instant(from)
                   : randomPeriod(from);
    }

    private static Period randomPeriod(Instant from) {
        Instant to = randomInstant(from);
        return new Period(from, to);
    }

    private static Agent randomAgent() {
        return new Organization.Builder()
            .withId(randomUri())
            .build();
    }

    private static Report randomReport()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Report.Builder()
            .withSeriesNumber(randomSeriesNumber())
            .withSeries(randomBookSeries())
            .withIsbnList(randomIsbnList())
            .withPublisher(randomPublishingHouse())
            .build();
    }

    private static String randomSeriesNumber() {
        return randomString();
    }

    private static Anthology randomChapter() {
        return new Anthology.Builder()
            .withId(randomUri())
            .build();
    }

    private static Book randomBook() {
        return new Book.BookBuilder()
            .withIsbnList(randomIsbnList())
            .withPublisher(randomPublishingHouse())
            .withSeries(randomBookSeries())
            .withSeriesNumber(randomSeriesNumber())
            .withRevision(Revision.values()[new Random().nextInt(Revision.values().length)])
            .build();
    }

    private static Series randomBookSeries() {
        return new Series(randomPublicationChannelsUri());
    }

    public static PublishingHouse randomPublishingHouse() {
        return new Publisher(randomPublicationChannelsUri());
    }

    private static List<String> randomIsbnList() {
        return List.of(randomIsbn13(), randomIsbn10());
    }

    private static Journal randomJournal() {
        return new Journal(randomPublicationChannelsUri());
    }

    private static URI randomPublicationChannelsUri() {
        return URI.create("https://api.dev.nva.aws.unit.no/publication-channels/" + UUID.randomUUID());
    }

    private static Artistic randomArtistic() {
        return new Artistic();
    }

    private static Place randomPlace() {
        return new UnconfirmedPlace(randomString(), randomString());
    }
}
