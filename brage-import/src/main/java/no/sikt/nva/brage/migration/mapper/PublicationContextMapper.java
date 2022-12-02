package no.sikt.nva.brage.migration.mapper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public final class PublicationContextMapper {

    public static final URI BASE_URL = URI.create("https://api.dev.nva.aws.unit.no/publication-channels");

    private PublicationContextMapper() {
    }

    public static PublicationContext buildPublicationContext(Record record)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        if (isNull(record.getPublication().getPublicationContext())) {
            return null;
        }
        if (isReport(record) || isBook(record)) {
            return buildPublicationContextWhenReportOrBook(record);
        }
        return null;
    }

    private static List<String> extractIsbnList(Record record) {
        return Collections.singletonList(record.getPublication().getIsbn());
    }

    private static String extractYear(Record record) {
        return record.getEntityDescription().getPublicationDate().getNva().getYear();
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

    private static PublicationContext buildPublicationContextWhenReportOrBook(Record record)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        return new Report.Builder()
                   .withPublisher(extractPublisher(record))
                   .withSeries(extractSeries(record))
                   .withIsbnList(extractIsbnList(record))
                   .withSeriesNumber(extractSeriesNumber(record))
                   .build();
    }

    private static String extractSeriesNumber(Record record) {
        var partOfSeriesValue = record.getPublication().getPartOfSeries();
        return extractPartOfSeriesValue(partOfSeriesValue);
    }

    @JacocoGenerated
    private static String extractPartOfSeriesValue(String partOfSeriesValue) {
        var potentialSeriesNumber = partOfSeriesValue.split(";")[1];
        if (nonNull(potentialSeriesNumber)) {
            return extractPotentialSeriesNumberValue(potentialSeriesNumber);
        }
        return null;
    }

    private static Series extractSeries(Record record) {
        var identifier = record.getPublication().getPublicationContext().getSeries().getId();
        var year = extractYear(record);
        return new Series(generatePublicationChannelUri(ChannelType.SERIES, identifier, year));
    }

    private static Publisher extractPublisher(Record record) {
        var identifier = record.getPublication().getPublicationContext().getPublisher().getId();
        var year = extractYear(record);
        return new Publisher(generatePublicationChannelUri(ChannelType.PUBLISHER, identifier, year));
    }

    private static URI generatePublicationChannelUri(ChannelType type, String publisherIdentifier,
                                                     String year) {
        return UriWrapper.fromUri(
                PublicationContextMapper.BASE_URL)
                   .addChild(type.getType())
                   .addChild(publisherIdentifier)
                   .addChild(year)
                   .getUri();
    }

    private static boolean isReport(Record record) {
        return NvaType.REPORT.getValue().equals(record.getType().getNva());
    }
}
