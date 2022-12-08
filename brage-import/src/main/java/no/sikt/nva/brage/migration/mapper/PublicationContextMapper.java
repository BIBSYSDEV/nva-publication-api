package no.sikt.nva.brage.migration.mapper;

import static java.util.Objects.isNull;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.lambda.PublicationContextException;
import no.sikt.nva.brage.migration.record.EntityDescription;
import no.sikt.nva.brage.migration.record.Publication;
import no.sikt.nva.brage.migration.record.PublicationDate;
import no.sikt.nva.brage.migration.record.PublicationDateNva;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.GeographicalContent;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.ResearchData;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public final class PublicationContextMapper {

    public static final URI BASE_URL = URI.create("https://api.dev.nva.aws.unit.no/publication-channels");
    public static final String NOT_SUPPORTED_TYPE = "Not supported type for creating publication context: ";

    private PublicationContextMapper() {
    }

    public static PublicationContext buildPublicationContext(Record record)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        if (isBook(record)) {
            return buildPublicationContextWhenBook(record);
        }
        if (isReport(record) || isResearchReport(record)) {
            return buildPublicationContextWhenReport(record);
        }
        if (isDegree(record)) {
            return buildPublicationContextWhenDegree(record);
        }
        if (isMap(record)) {
            return buildPublicationContextWhenMap(record);
        }
        if (isDataset(record)) {
            return buildPublicationContextWhenDataSet(record);
        }
        if (isJournalArticle(record)) {
            return buildPublicationContextWhenJournalArticle(record);
        } else {
            throw new PublicationContextException(NOT_SUPPORTED_TYPE + record.getType().getNva());
        }
    }

    private static PublicationContext buildPublicationContextWhenJournalArticle(Record record) {
        return extractJournal(record);
    }

    private static boolean isJournalArticle(Record record) {
        return NvaType.JOURNAL_ARTICLE.getValue().equals(record.getType().getNva());
    }

    private static PublicationContext buildPublicationContextWhenDataSet(Record record) {
        return new ResearchData(extractPublisher(record));
    }

    private static boolean isDataset(Record record) {
        return NvaType.DATASET.getValue().equals(record.getType().getNva());
    }

    private static boolean isMap(Record record) {
        return NvaType.MAP.getValue().equals(record.getType().getNva());
    }

    private static PublicationContext buildPublicationContextWhenMap(Record record) {
        return new GeographicalContent(extractPublisher(record));
    }

    private static boolean isDegree(Record record) {
        return NvaType.BACHELOR_THESIS.getValue().equals(record.getType().getNva())
               || NvaType.MASTER_THESIS.getValue().equals(record.getType().getNva())
               || NvaType.DOCTORAL_THESIS.getValue().equals(record.getType().getNva());
    }

    private static PublicationContext buildPublicationContextWhenDegree(Record record)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Degree.Builder()
                   .withIsbnList(extractIsbnList(record))
                   .withSeries(extractSeries(record))
                   .withPublisher(extractPublisher(record))
                   .withSeriesNumber(extractSeriesNumber(record))
                   .build();
    }

    private static boolean isResearchReport(Record record) {
        return NvaType.RESEARCH_REPORT.getValue().equals(record.getType().getNva());
    }

    private static List<String> extractIsbnList(Record record) {
        return isNull(record.getPublication().getIsbn())
                   ? Collections.emptyList()
                   : Collections.singletonList(record.getPublication().getIsbn());
    }

    private static String extractYear(Record record) {
        return Optional.ofNullable(record.getEntityDescription())
                   .map(EntityDescription::getPublicationDate)
                   .map(PublicationDate::getNva)
                   .map(PublicationDateNva::getYear)
                   .orElse(null);
    }

    private static String extractPotentialSeriesNumberValue(String potentialSeriesNumber) {
        if (potentialSeriesNumber.contains(":")) {
            var seriesNumberAndYear = Arrays.asList(potentialSeriesNumber.split(":"));
            return Collections.max(seriesNumberAndYear);
        }

        if (potentialSeriesNumber.contains("/")) {
            var seriesNumberAndYear = Arrays.asList(potentialSeriesNumber.split("/"));
            return Collections.max(seriesNumberAndYear);
        }
        return potentialSeriesNumber;
    }

    private static boolean isBook(Record record) {
        return NvaType.BOOK.getValue().equals(record.getType().getNva());
    }

    private static PublicationContext buildPublicationContextWhenBook(Record record)
        throws InvalidIsbnException {
        return new Book.BookBuilder()
                   .withPublisher(extractPublisher(record))
                   .withSeries(extractSeries(record))
                   .withIsbnList(extractIsbnList(record))
                   .withSeriesNumber(extractSeriesNumber(record))
                   .build();
    }

    private static PublicationContext buildPublicationContextWhenReport(Record record)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        return new Report.Builder()
                   .withPublisher(extractPublisher(record))
                   .withSeries(extractSeries(record))
                   .withIsbnList(extractIsbnList(record))
                   .withSeriesNumber(extractSeriesNumber(record))
                   .build();
    }

    private static String extractSeriesNumber(Record record) {
        return Optional.ofNullable(record.getPublication())
                   .map(Publication::getPartOfSeries)
                   .map(PublicationContextMapper::extractPartOfSeriesValue)
                   .orElse(null);
    }

    @JacocoGenerated
    private static String extractPartOfSeriesValue(String partOfSeriesValue) {
        return Optional.ofNullable(partOfSeriesValue.split(";")[1])
                   .map(PublicationContextMapper::extractPotentialSeriesNumberValue)
                   .orElse(null);
    }

    private static Series extractSeries(Record record) {
        return Optional.ofNullable(record.getPublication().getPublicationContext())
                   .map(no.sikt.nva.brage.migration.record.PublicationContext::getSeries)
                   .map(no.sikt.nva.brage.migration.record.Series::getId)
                   .map(id -> generateSeries(id, extractYear(record))).orElse(null);
    }

    private static Publisher extractPublisher(Record record) {
        return Optional.ofNullable(record.getPublication().getPublicationContext())
                   .map(no.sikt.nva.brage.migration.record.PublicationContext::getPublisher)
                   .map(no.sikt.nva.brage.migration.record.Publisher::getId)
                   .map(id -> generatePublisher(id, extractYear(record)))
                   .orElse(null);
    }

    private static Journal extractJournal(Record record) {
        return Optional.ofNullable(record.getPublication().getPublicationContext())
                   .map(no.sikt.nva.brage.migration.record.PublicationContext::getJournal)
                   .map(no.sikt.nva.brage.migration.record.Journal::getId)
                   .map(id -> generateJournal(id, extractYear(record))).orElse(null);
    }

    private static Publisher generatePublisher(String publisherIdentifier, String year) {
        return new Publisher(UriWrapper.fromUri(PublicationContextMapper.BASE_URL)
                                 .addChild(ChannelType.PUBLISHER.getType())
                                 .addChild(publisherIdentifier)
                                 .addChild(year)
                                 .getUri());
    }

    private static Journal generateJournal(String journalIdentifier, String year) {
        return new Journal(UriWrapper.fromUri(PublicationContextMapper.BASE_URL)
                               .addChild(ChannelType.JOURNAL.getType())
                               .addChild(journalIdentifier)
                               .addChild(year)
                               .getUri());
    }

    private static Series generateSeries(String seriesIdentifier, String year) {
        return new Series(UriWrapper.fromUri(PublicationContextMapper.BASE_URL)
                              .addChild(ChannelType.SERIES.getType())
                              .addChild(seriesIdentifier)
                              .addChild(year)
                              .getUri());
    }

    private static boolean isReport(Record record) {
        return NvaType.REPORT.getValue().equals(record.getType().getNva());
    }
}
