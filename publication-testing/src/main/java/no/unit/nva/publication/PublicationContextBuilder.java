package no.unit.nva.publication;

import static no.unit.nva.publication.RandomUtils.randomLabel;
import static no.unit.nva.publication.RandomUtils.randomLabels;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn13;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import no.unit.nva.model.Agent;
import no.unit.nva.model.Organization;
import no.unit.nva.model.contexttypes.ArtisticDesign;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Chapter;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Event;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.place.Place;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.contexttypes.venue.Venue;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.model.pages.TemporalExtent;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class PublicationContextBuilder {


    public static PublicationContext randomPublicationContext(Class<?> publicationInstance) {
        String className = publicationInstance.getSimpleName();
        switch (className) {
            case "ArtisticDesignClothingDesign":
            case "ArtisticDesignExhibition":
            case "ArtisticDesignGraphicDesign":
            case "ArtisticDesignIllustration":
            case "ArtisticDesignInteractionDesign":
            case "ArtisticDesignInteriorDesign":
            case "ArtisticDesignLightDesign":
            case "ArtisticDesignOther":
            case "ArtisticDesignProductDesign":
            case "ArtisticDesignServiceDesign":
            case "ArtisticDesignWebDesign":
                return randomArtisticDesign();

            case "FeatureArticle":
            case "JournalCorrigendum":
            case "JournalArticle":
            case "JournalInterview":
            case "JournalLetter":
            case "JournalLeader":
            case "JournalReview":
            case "JournalShortCommunication":
                return randomJournal();
            case "BookAnthology":
            case "BookAbstracts":
            case "BookMonograph":
            case "OtherStudentWork":
                return attempt(PublicationContextBuilder::randomBook).orElseThrow();
            case "DegreeBachelor":
            case "DegreeMaster":
            case "DegreePhd":
                return attempt(PublicationContextBuilder::randomDegree).orElseThrow();
            case "ChapterArticle":
                return randomChapter();
            case "ReportBasic":
            case "ReportPolicy":
            case "ReportResearch":
            case "ReportWorkingPaper":
                return attempt(PublicationContextBuilder::randomReport).orElseThrow();
            case "ConferenceLecture":
            case "ConferencePoster":
            case "Lecture":
            case "OtherPresentation":
                return randomPresentation();
            default:
                throw new UnsupportedOperationException("Publication instance not supported: " + className);
        }
    }

    private static Degree randomDegree() throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Degree.Builder()
            .withSeriesNumber(randomSeriesNumber())
            .withSeries(randomBookSeries())
            .withIsbnList(randomIsbnList())
            .withPublisher(randomPublishingHouse())
            .build();
    }

    private static Event randomPresentation() {
        return new Event.Builder()
            .withAgent(randomAgent())
            .withLabel(randomLabel())
            .withPlace(randomPlace())
            .withProduct(PublicationGenerator.randomPublication(FeatureArticle.class))
            .withTime(randomTemporalExtent())
            .build();
    }

    private static TemporalExtent randomTemporalExtent() {
        Instant fromInstant = randomInstant();
        LocalDateTime from = LocalDateTime.ofInstant(fromInstant, ZoneId.systemDefault());
        LocalDateTime to = LocalDateTime.ofInstant(randomInstant(fromInstant), ZoneId.systemDefault());
        return new TemporalExtent(from, to);
    }

    private static Agent randomAgent() {
        return new Organization.Builder()
            .withId(randomUri())
            .withLabels(randomLabels())
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

    private static Chapter randomChapter() {
        return new Chapter.Builder()
            .withPartOf(randomUri())
            .build();
    }

    private static Book randomBook() throws InvalidIsbnException {
        return new Book.BookBuilder()
            .withIsbnList(randomIsbnList())
            .withPublisher(randomPublishingHouse())
            .withSeries(randomBookSeries())
            .withSeriesNumber(randomSeriesNumber())
            .build();
    }

    private static Series randomBookSeries() {
        return new Series(randomPublicationChannelsUri());
    }

    private static PublishingHouse randomPublishingHouse() {
        return new Publisher(randomPublicationChannelsUri());
    }

    private static List<String> randomIsbnList() {
        return List.of(randomIsbn13());
    }

    private static Journal randomJournal() {
        return new Journal(randomPublicationChannelsUri().toString());
    }

    private static URI randomPublicationChannelsUri() {
        return URI.create("https://api.dev.nva.aws.unit.no/publication-channels/"+randomString());
    }

    private static ArtisticDesign randomArtisticDesign() {
        return new ArtisticDesign(randomVenues());
    }

    private static List<Venue> randomVenues() {
        return List.of(randomVenue(), randomVenue());
    }

    private static Venue randomVenue() {
        return new Venue(randomPlace(), randomInteger());
    }

    private static Place randomPlace() {
        return new UnconfirmedPlace(randomString(), randomString());
    }
}

